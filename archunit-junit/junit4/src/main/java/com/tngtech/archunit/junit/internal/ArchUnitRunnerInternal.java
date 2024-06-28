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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.junit.CacheMode;
import com.tngtech.archunit.junit.LocationProvider;
import org.junit.runner.Description;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import static com.tngtech.archunit.junit.internal.ArchRuleDeclaration.elementShouldBeIgnored;
import static com.tngtech.archunit.junit.internal.ArchRuleDeclaration.toDeclarations;
import static com.tngtech.archunit.junit.internal.ArchTestExecution.getValue;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

final class ArchUnitRunnerInternal extends ParentRunner<ArchTestExecution> implements ArchUnitRunner.InternalRunner<ArchTestExecution> {
    @SuppressWarnings("FieldMayBeFinal")
    private SharedCache cache = new SharedCache(); // NOTE: We want to change this in tests -> no static/final reference

    ArchUnitRunnerInternal(Class<?> testClass) throws InitializationError {
        super(testClass);
        checkAnnotation(testClass);

        try {
            ArchUnitSystemPropertyTestFilterJunit4.filter(this);
        } catch (NoTestsRemainException e) {
            throw new InitializationError(e);
        }
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
    public Statement classBlock(RunNotifier notifier) {
        Statement statement = super.classBlock(notifier);
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    statement.evaluate();
                } finally {
                    cache.clear(getTestClass().getJavaClass());
                }
            }
        };
    }

    @Override
    public List<ArchTestExecution> getChildren() {
        List<ArchTestExecution> children = new ArrayList<>();
        children.addAll(findArchRuleFields());
        children.addAll(findArchRuleMethods());
        return children;
    }

    private Collection<ArchTestExecution> findArchRuleFields() {
        return getTestClass().getAnnotatedFields(ArchTest.class).stream()
                .flatMap(ruleField -> findArchRulesIn(ruleField).stream())
                .collect(toList());
    }

    private Set<ArchTestExecution> findArchRulesIn(FrameworkField ruleField) {
        boolean ignore = elementShouldBeIgnored(getTestClass().getJavaClass(), ruleField.getField());
        if (ruleField.getType() == ArchTests.class) {
            return asTestExecutions(getArchTests(ruleField.getField()), ignore);
        }
        return singleton(new ArchRuleExecution(singletonList(getTestClass().getJavaClass()), getTestClass().getJavaClass(), ruleField.getField(), ignore));
    }

    private Set<ArchTestExecution> asTestExecutions(ArchTests archTests, boolean forceIgnore) {
        ExecutionTransformer executionTransformer = new ExecutionTransformer();
        for (ArchRuleDeclaration<?> declaration : toDeclarations(archTests, singletonList(getTestClass().getJavaClass()), ArchTest.class, forceIgnore)) {
            declaration.handleWith(executionTransformer);
        }
        return executionTransformer.getExecutions();
    }

    private ArchTests getArchTests(Field field) {
        return getValue(field, field.getDeclaringClass());
    }

    private Collection<ArchTestExecution> findArchRuleMethods() {
        List<ArchTestExecution> result = new ArrayList<>();
        for (FrameworkMethod testMethod : getTestClass().getAnnotatedMethods(ArchTest.class)) {
            boolean ignore = elementShouldBeIgnored(getTestClass().getJavaClass(), testMethod.getMethod());
            result.add(new ArchTestMethodExecution(singletonList(getTestClass().getJavaClass()), getTestClass().getJavaClass(), testMethod.getMethod(), ignore));
        }
        return result;
    }

    @Override
    public Description describeChild(ArchTestExecution child) {
        return child.describeSelf();
    }

    @Override
    public void runChild(ArchTestExecution child, RunNotifier notifier) {
        if (child.ignore()) {
            notifier.fireTestIgnored(describeChild(child));
        } else {
            notifier.fireTestStarted(describeChild(child));
            Class<?> testClass = getTestClass().getJavaClass();
            JavaClasses classes = cache.get().getClassesToAnalyzeFor(testClass, new JUnit4ClassAnalysisRequest(testClass));
            child.evaluateOn(classes).notify(notifier);
            notifier.fireTestFinished(describeChild(child));
        }
    }

    static class SharedCache {
        private static final ClassCache cache = new ClassCache();

        ClassCache get() {
            return cache;
        }

        void clear(Class<?> testClass) {
            cache.clear(testClass);
        }
    }

    private static class ExecutionTransformer implements ArchRuleDeclaration.Handler {
        private final ImmutableSet.Builder<ArchTestExecution> executions = ImmutableSet.builder();

        @Override
        public void handleFieldDeclaration(List<Class<?>> testClassPath, Field field, Class<?> fieldOwner, boolean ignore) {
            executions.add(new ArchRuleExecution(testClassPath, fieldOwner, field, ignore));
        }

        @Override
        public void handleMethodDeclaration(List<Class<?>> testClassPath, Method method, Class<?> methodOwner, boolean ignore) {
            executions.add(new ArchTestMethodExecution(testClassPath, methodOwner, method, ignore));
        }

        Set<ArchTestExecution> getExecutions() {
            return executions.build();
        }
    }

    private static class JUnit4ClassAnalysisRequest implements ClassAnalysisRequest {
        private final AnalyzeClasses analyzeClasses;

        JUnit4ClassAnalysisRequest(Class<?> testClass) {
            analyzeClasses = checkAnnotation(testClass);
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
}
