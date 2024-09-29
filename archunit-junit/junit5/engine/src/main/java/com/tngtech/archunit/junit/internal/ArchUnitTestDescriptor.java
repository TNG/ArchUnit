/*
 * Copyright 2014-2024 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.junit.internal;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.junit.CacheMode;
import com.tngtech.archunit.junit.LocationProvider;
import com.tngtech.archunit.junit.engine_api.FieldSource;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tngtech.archunit.junit.internal.DisplayNameResolver.determineDisplayName;
import static com.tngtech.archunit.junit.internal.ReflectionUtils.getAllFields;
import static com.tngtech.archunit.junit.internal.ReflectionUtils.getAllMethods;
import static com.tngtech.archunit.junit.internal.ReflectionUtils.getValueOrThrowException;
import static com.tngtech.archunit.junit.internal.ReflectionUtils.invokeMethod;
import static com.tngtech.archunit.junit.internal.ReflectionUtils.withAnnotation;

class ArchUnitTestDescriptor extends AbstractArchUnitTestDescriptor implements CreatesChildren {
    private static final Logger LOG = LoggerFactory.getLogger(ArchUnitTestDescriptor.class);

    static final String CLASS_SEGMENT_TYPE = "class";
    static final String FIELD_SEGMENT_TYPE = "field";
    static final String METHOD_SEGMENT_TYPE = "method";

    private final Class<?> testClass;
    @SuppressWarnings("FieldMayBeFinal") // We want to change this in tests
    private ClassCache classCache;

    private ArchUnitTestDescriptor(ElementResolver resolver, Class<?> testClass, ClassCache classCache) {
        super(resolver.getUniqueId(), testClass.getSimpleName(), ClassSource.from(testClass), testClass);
        this.testClass = testClass;
        this.classCache = classCache;
    }

    static void resolve(TestDescriptor parent, ElementResolver resolver, ClassCache classCache) {
        resolver.resolveClass()
                .ifRequestedAndResolved(CreatesChildren::createChildren)
                .ifRequestedButUnresolved((clazz, childResolver) -> createTestDescriptor(parent, classCache, clazz, childResolver));
    }

    private static void createTestDescriptor(TestDescriptor parent, ClassCache classCache, Class<?> clazz, ElementResolver childResolver) {
        List<AnalyzeClasses> analyzeClasses = new AnnotationFinder<>(AnalyzeClasses.class).findAnnotationsOn(clazz);
        if (analyzeClasses.isEmpty()) {
            LOG.warn("Class {} is not annotated with @{} and thus cannot run as a top level test. "
                            + "This warning can be ignored if {} is only used as part of a rules library included via {}.in({}.class).",
                    clazz.getName(), AnalyzeClasses.class.getSimpleName(),
                    clazz.getSimpleName(), ArchTests.class.getSimpleName(), clazz.getSimpleName());

            return;
        }

        ArchTestInitializationException.check(analyzeClasses.size() == 1,
                "Multiple @%s annotations found on %s! This is not supported at the moment.",
                AnalyzeClasses.class.getSimpleName(), clazz.getSimpleName());

        ArchUnitTestDescriptor classDescriptor = new ArchUnitTestDescriptor(childResolver, clazz, classCache);
        parent.addChild(classDescriptor);
        classDescriptor.createChildren(childResolver);
    }

    @Override
    public void createChildren(ElementResolver resolver) {
        Supplier<JavaClasses> classes = () -> classCache.getClassesToAnalyzeFor(testClass, new JUnit5ClassAnalysisRequest(testClass));

        getAllFields(testClass, withAnnotation(ArchTest.class))
                .forEach(field -> resolveField(resolver, classes, new TestMember<>(testClass, field)));
        getAllMethods(testClass, withAnnotation(ArchTest.class))
                .forEach(method -> resolveMethod(resolver, classes, new TestMember<>(testClass, method)));
    }

    private void resolveField(ElementResolver resolver, Supplier<JavaClasses> classes, TestMember<Field> field) {
        resolver.resolveField(field.member)
                .ifUnresolved(childResolver -> resolveChildren(this, childResolver, field, classes));
    }

    private void resolveMethod(ElementResolver resolver, Supplier<JavaClasses> classes, TestMember<Method> method) {
        resolver.resolveMethod(method.member)
                .ifUnresolved(childResolver -> addChild(new ArchUnitMethodDescriptor(getUniqueId(), method, classes)));
    }

    private static void resolveChildren(
            TestDescriptor parent, ElementResolver resolver, TestMember<Field> field, Supplier<JavaClasses> classes) {

        if (ArchTests.class.isAssignableFrom(field.member.getType())) {
            resolveArchRules(parent, resolver, field, classes);
        } else {
            parent.addChild(new ArchUnitRuleDescriptor(resolver.getUniqueId(), getValue(field), classes, field));
        }
    }

    private static <T> T getValue(TestMember<Field> field) {
        return getValueOrThrowException(field.member, field.owner, ArchTestInitializationException::new);
    }

    private static void resolveArchRules(
            TestDescriptor parent, ElementResolver resolver, TestMember<Field> field, Supplier<JavaClasses> classes) {

        DeclaredArchTests archTests = getDeclaredArchTests(field);

        resolver.resolveClass(archTests.getDefinitionLocation())
                .ifRequestedAndResolved(CreatesChildren::createChildren)
                .ifRequestedButUnresolved((clazz, childResolver) -> {
                    ArchUnitArchTestsDescriptor rulesDescriptor = new ArchUnitArchTestsDescriptor(childResolver, archTests, classes, field);
                    parent.addChild(rulesDescriptor);
                    rulesDescriptor.createChildren(childResolver);
                });
    }

    private static DeclaredArchTests getDeclaredArchTests(TestMember<Field> field) {
        return new DeclaredArchTests(getValue(field));
    }

    @Override
    public Type getType() {
        return Type.CONTAINER;
    }

    @Override
    public void after(ArchUnitEngineExecutionContext context) {
        classCache.clear(testClass);
    }

    private static class ArchUnitRuleDescriptor extends AbstractArchUnitTestDescriptor {
        private final ArchRule rule;
        private final Supplier<JavaClasses> classes;

        ArchUnitRuleDescriptor(UniqueId uniqueId, ArchRule rule, Supplier<JavaClasses> classes, TestMember<Field> field) {
            super(uniqueId, determineDisplayName(formatWithPath(uniqueId, field.getName())), FieldSource.from(field.member), field.member);
            this.rule = rule;
            this.classes = classes;
        }

        @Override
        public Type getType() {
            return Type.TEST;
        }

        @Override
        public ArchUnitEngineExecutionContext execute(ArchUnitEngineExecutionContext context, DynamicTestExecutor dynamicTestExecutor) {
            rule.check(classes.get());
            return context;
        }
    }

    private static class ArchUnitMethodDescriptor extends AbstractArchUnitTestDescriptor {
        private final TestMember<Method> method;
        private final Supplier<JavaClasses> classes;

        ArchUnitMethodDescriptor(UniqueId uniqueId, TestMember<Method> method, Supplier<JavaClasses> classes) {
            super(uniqueId.append("method", method.member.getName()),
                    determineDisplayName(formatWithPath(uniqueId, method.member.getName())),
                    MethodSource.from(method.member),
                    method.member);

            validate(method.member);

            this.method = method;
            this.classes = classes;
        }

        private void validate(Method method) {
            ArchTestInitializationException.check(
                    method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(JavaClasses.class),
                    "@%s Method %s.%s must have exactly one parameter of type %s",
                    ArchTest.class.getSimpleName(), method.getDeclaringClass().getSimpleName(), method.getName(), JavaClasses.class.getName());
        }

        @Override
        public Type getType() {
            return Type.TEST;
        }

        @Override
        public ArchUnitEngineExecutionContext execute(ArchUnitEngineExecutionContext context, DynamicTestExecutor dynamicTestExecutor) {
            invokeMethod(method.member, method.owner, classes.get());
            return context;
        }
    }

    private static class ArchUnitArchTestsDescriptor extends AbstractArchUnitTestDescriptor implements CreatesChildren {
        private final DeclaredArchTests archTests;
        private final Supplier<JavaClasses> classes;

        ArchUnitArchTestsDescriptor(ElementResolver resolver, DeclaredArchTests archTests, Supplier<JavaClasses> classes, TestMember<Field> field) {

            super(resolver.getUniqueId(),
                    archTests.getDisplayName(),
                    noSource(),
                    field.member,
                    archTests.getDefinitionLocation());
            this.archTests = archTests;
            this.classes = classes;
        }

        /**
         * We don't pass a ClassSource for intermediary descriptors or it will be used as test location by test executors.
         * We want the root class declaring <code>@AnalyzeClasses</code> to be used for this.
         */
        private static TestSource noSource() {
            return null;
        }

        @Override
        public void createChildren(ElementResolver resolver) {
            archTests.handleFields(field ->
                    resolver.resolve(
                            FIELD_SEGMENT_TYPE,
                            field.getName(),
                            childResolver -> resolveChildren(field, childResolver)));

            archTests.handleMethods(method ->
                    resolver.resolve(
                            METHOD_SEGMENT_TYPE,
                            method.getName(),
                            childResolver -> addChild(method)));
        }

        private void resolveChildren(Field field, ElementResolver childResolver) {
            ArchUnitTestDescriptor.resolveChildren(
                    this,
                    childResolver,
                    new TestMember<>(archTests.getDefinitionLocation(), field),
                    classes);
        }

        private void addChild(Method method) {
            addChild(new ArchUnitMethodDescriptor(
                    getUniqueId(),
                    new TestMember<>(archTests.getDefinitionLocation(), method),
                    classes));
        }

        @Override
        public Type getType() {
            return Type.CONTAINER;
        }
    }

    private static class DeclaredArchTests {
        private final ArchTests archTests;

        DeclaredArchTests(ArchTests archTests) {
            this.archTests = archTests;
        }

        Class<?> getDefinitionLocation() {
            return archTests.getDefinitionLocation();
        }

        String getDisplayName() {
            return archTests.getDefinitionLocation().getSimpleName();
        }

        void handleFields(Consumer<? super Field> doWithField) {
            getAllFields(archTests.getDefinitionLocation(), withAnnotation(ArchTest.class)).forEach(doWithField);
        }

        void handleMethods(Consumer<? super Method> doWithMethod) {
            getAllMethods(archTests.getDefinitionLocation(), withAnnotation(ArchTest.class)).forEach(doWithMethod);
        }
    }

    private static class JUnit5ClassAnalysisRequest implements ClassAnalysisRequest {
        private final AnalyzeClasses analyzeClasses;

        JUnit5ClassAnalysisRequest(Class<?> testClass) {
            analyzeClasses = checkAnnotation(testClass);
        }

        private static AnalyzeClasses checkAnnotation(Class<?> testClass) {
            List<AnalyzeClasses> analyzeClasses = new AnnotationFinder<>(AnalyzeClasses.class).findAnnotationsOn(testClass);
            ArchTestInitializationException.check(!analyzeClasses.isEmpty(),
                    "Class %s must be annotated with @%s",
                    testClass.getSimpleName(), AnalyzeClasses.class.getSimpleName());
            ArchTestInitializationException.check(analyzeClasses.size() == 1,
                    "Multiple @%s annotations found on %s! This is not supported at the moment.",
                    AnalyzeClasses.class.getSimpleName(), testClass.getSimpleName());
            return analyzeClasses.get(0);
        }

        @Override
        public String[] getPackageNames() {
            return analyzeClasses.packages();
        }

        @Override
        public Class<?>[] getPackageRoots() {
            return analyzeClasses.packagesOf();
        }

        @Override
        public Class<? extends LocationProvider>[] getLocationProviders() {
            return analyzeClasses.locations();
        }

        @Override
        public Class<? extends ImportOption>[] getImportOptions() {
            return analyzeClasses.importOptions();
        }

        @Override
        public CacheMode getCacheMode() {
            return analyzeClasses.cacheMode();
        }

        @Override
        public boolean scanWholeClasspath() {
            return analyzeClasses.wholeClasspath();
        }
    }

    private static class TestMember<MEMBER extends AccessibleObject & Member> {
        final Class<?> owner;
        final MEMBER member;

        TestMember(Class<?> owner, MEMBER member) {
            this.owner = owner;
            this.member = member;
        }

        String getName() {
            return member.getName();
        }
    }
}
