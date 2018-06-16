/*
 * Copyright 2018 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.junit;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Suppliers.memoize;
import static com.tngtech.archunit.junit.ArchRuleDeclaration.toDeclarations;
import static com.tngtech.archunit.junit.ReflectionUtils.getAllFields;
import static com.tngtech.archunit.junit.ReflectionUtils.getAllMethods;
import static com.tngtech.archunit.junit.ReflectionUtils.getValue;
import static com.tngtech.archunit.junit.ReflectionUtils.withAnnotation;
import static java.lang.reflect.Modifier.isStatic;

class ArchUnitTestDescriptor extends AbstractArchUnitTestDescriptor {
    private final Class<?> testClass;
    private ClassCache classCache;

    ArchUnitTestDescriptor(UniqueId uniqueId, Class<?> testClass, ClassCache classCache) {
        super(uniqueId.append("class", testClass.getName()), testClass.getSimpleName(), ClassSource.from(testClass), testClass);
        this.testClass = testClass;
        this.classCache = classCache;

        createChildren();
    }

    private void createChildren() {
        Supplier<JavaClasses> classes =
                memoize(() -> classCache.getClassesToAnalyzeFor(testClass, new JUnit5ClassAnalysisRequest(testClass)))::get;
        getAllFields(testClass, withAnnotation(ArchTest.class))
                .forEach(field -> addChild(descriptorFor(this, getUniqueId(), testClass, field, classes)));
        getAllMethods(testClass, withAnnotation(ArchTest.class))
                .forEach(method -> addChild(new ArchUnitMethodDescriptor(this, getUniqueId(), method, classes)));
    }

    private static TestDescriptor descriptorFor(TestDescriptor parent, UniqueId uniqueId, Class<?> testClass, Field field,
            Supplier<JavaClasses> classes) {
        return ArchRules.class.isAssignableFrom(field.getType())
                ? new ArchUnitRulesDescriptor(parent, uniqueId, getDeclaredRules(testClass, field), classes, field)
                : new ArchUnitRuleDescriptor(parent, uniqueId, getValue(field, null), classes, field);
    }

    private static DeclaredArchRules getDeclaredRules(Class<?> testClass, Field field) {
        return new DeclaredArchRules(testClass, getValue(field, null));
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

        ArchUnitRuleDescriptor(TestDescriptor parent, UniqueId uniqueId, ArchRule rule, Supplier<JavaClasses> classes, Field field) {
            super(uniqueId.append("field", field.getName()), field.getName(), FieldSource.from(field), field);
            setParent(parent);
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
        private final Method method;
        private final Supplier<JavaClasses> classes;

        ArchUnitMethodDescriptor(TestDescriptor parent, UniqueId uniqueId, Method method,
                Supplier<JavaClasses> classes) {
            super(uniqueId.append("method", method.getName()), method.getName(), MethodSource.from(method), method);
            validate(method);

            setParent(parent);
            this.method = method;
            this.classes = classes;
            this.method.setAccessible(true);
        }

        private void validate(Method method) {
            ArchTestInitializationException.check(
                    isStatic(method.getModifiers()),
                    "@%s Method %s.%s must be static",
                    ArchTest.class.getSimpleName(), method.getDeclaringClass().getSimpleName(), method.getName());

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
            unwrapException(() -> method.invoke(null, classes.get()))
                    .ifPresent(this::rethrowUnchecked);
            return context;
        }

        // Exceptions occurring during reflective calls are wrapped within an InvocationTargetException
        private Optional<Throwable> unwrapException(Callable<?> callback) {
            try {
                callback.call();
                return Optional.empty();
            } catch (Exception e) {
                Throwable throwable = e;
                while (throwable instanceof InvocationTargetException) {
                    throwable = ((InvocationTargetException) e).getTargetException();
                }
                return Optional.of(throwable);
            }
        }

        // Certified Hack(TM) to rethrow any exception unchecked. Uses a hole in the JLS with respect to Generics.
        @SuppressWarnings("unchecked")
        private <T extends Throwable> void rethrowUnchecked(Throwable throwable) throws T {
            throw (T) throwable;
        }
    }

    private static class ArchUnitRulesDescriptor extends AbstractArchUnitTestDescriptor {

        ArchUnitRulesDescriptor(TestDescriptor parent, UniqueId uniqueId, DeclaredArchRules rules,
                Supplier<JavaClasses> classes, Field field) {
            super(uniqueId
                            .append("field", field.getName())
                            .append("class", rules.getDefinitionLocation().getName()),
                    rules.getDisplayName(),
                    ClassSource.from(rules.getDefinitionLocation()),
                    field);

            setParent(parent);

            rules.forEachDeclaration(declaration -> declaration.handleWith(new ArchRuleDeclaration.Handler() {
                @Override
                public void handleFieldDeclaration(Field field, boolean ignore) {
                    addChild(descriptorFor(ArchUnitRulesDescriptor.this, getUniqueId(), rules.getTestClass(), field, classes));
                }

                @Override
                public void handleMethodDeclaration(Method method, boolean ignore) {
                    addChild(new ArchUnitMethodDescriptor(ArchUnitRulesDescriptor.this, getUniqueId(), method, classes));
                }
            }));
        }

        @Override
        public Type getType() {
            return Type.CONTAINER;
        }
    }

    private static class DeclaredArchRules {
        private final Class<?> testClass;
        private final ArchRules rules;

        DeclaredArchRules(Class<?> testClass, ArchRules rules) {
            this.testClass = testClass;
            this.rules = rules;
        }

        Class<?> getTestClass() {
            return testClass;
        }

        Class<?> getDefinitionLocation() {
            return rules.getDefinitionLocation();
        }

        String getDisplayName() {
            return rules.getDefinitionLocation().getSimpleName();
        }

        void forEachDeclaration(Consumer<ArchRuleDeclaration<?>> doWithDeclaration) {
            toDeclarations(rules, testClass, false).forEach(doWithDeclaration);
        }
    }

    private static class JUnit5ClassAnalysisRequest implements ClassAnalysisRequest {
        private final AnalyzeClasses analyzeClasses;

        JUnit5ClassAnalysisRequest(Class<?> testClass) {
            analyzeClasses = checkAnnotation(testClass);
        }

        private static AnalyzeClasses checkAnnotation(Class<?> testClass) {
            AnalyzeClasses analyzeClasses = testClass.getAnnotation(AnalyzeClasses.class);
            checkArgument(analyzeClasses != null,
                    "Class %s must be annotated with @%s",
                    testClass.getSimpleName(), AnalyzeClasses.class.getSimpleName());
            return analyzeClasses;
        }

        @Override
        public String[] getPackages() {
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
    }
}
