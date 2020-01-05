/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.Internal;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.junit.ArchRuleDeclaration.elementShouldBeIgnored;
import static com.tngtech.archunit.junit.ArchRuleDeclaration.toDeclarations;
import static com.tngtech.archunit.junit.ArchTestExecution.getValue;

/**
 * Evaluates {@link ArchRule ArchRules} against the classes inside of the packages specified via
 * {@link AnalyzeClasses @AnalyzeClasses} on the annotated test class.
 * <p>
 * NOTE: The runner demands {@link AnalyzeClasses @AnalyzeClasses} to be present on the respective test class.
 * </p>
 * Example:
 * <pre><code>
 *{@literal @}RunWith(ArchUnitRunner.class)
 *{@literal @}AnalyzeClasses(packages = "com.example")
 * public class SomeArchTest {
 *    {@literal @}ArchTest
 *     public static final ArchRule some_rule = //...
 * }
 * </code></pre>
 *
 * The runner will cache classes between test runs, for details please refer to {@link ClassCache}.
 */
@PublicAPI(usage = ACCESS)
public class ArchUnitRunner extends ParentRunner<ArchTestExecution> {
    private SharedCache cache = new SharedCache(); // NOTE: We want to change this in tests -> no static/final reference

    @Internal
    public ArchUnitRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
        checkAnnotation(testClass);
    }

    private static AnalyzeClasses checkAnnotation(Class<?> testClass) {
        AnalyzeClasses analyzeClasses = testClass.getAnnotation(AnalyzeClasses.class);
        ArchTestInitializationException.check(analyzeClasses != null,
                "Class %s must be annotated with @%s",
                testClass.getSimpleName(), AnalyzeClasses.class.getSimpleName());
        return analyzeClasses;
    }

    @Override
    protected Statement classBlock(RunNotifier notifier) {
        final Statement statement = super.classBlock(notifier);
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
    protected List<ArchTestExecution> getChildren() {
        List<ArchTestExecution> children = new ArrayList<>();
        children.addAll(findArchRuleFields());
        children.addAll(findArchRuleMethods());
        return children;
    }

    private Collection<ArchTestExecution> findArchRuleFields() {
        List<ArchTestExecution> result = new ArrayList<>();
        for (FrameworkField ruleField : getTestClass().getAnnotatedFields(ArchTest.class)) {
            result.addAll(findArchRulesIn(ruleField));
        }
        return result;
    }

    private Set<ArchTestExecution> findArchRulesIn(FrameworkField ruleField) {
        boolean ignore = elementShouldBeIgnored(ruleField.getField());
        if (ruleField.getType() == ArchRules.class) {
            return asTestExecutions(getArchRules(ruleField.getField()), ignore);
        }
        return Collections.<ArchTestExecution>singleton(new ArchRuleExecution(getTestClass().getJavaClass(), ruleField.getField(), ignore));
    }

    private Set<ArchTestExecution> asTestExecutions(ArchRules archRules, boolean forceIgnore) {
        ExecutionTransformer executionTransformer = new ExecutionTransformer();
        for (ArchRuleDeclaration<?> declaration : toDeclarations(archRules, getTestClass().getJavaClass(), ArchTest.class, forceIgnore)) {
            declaration.handleWith(executionTransformer);
        }
        return executionTransformer.getExecutions();
    }

    private ArchRules getArchRules(Field field) {
        return getValue(field, field.getDeclaringClass());
    }

    private Collection<ArchTestExecution> findArchRuleMethods() {
        List<ArchTestExecution> result = new ArrayList<>();
        for (FrameworkMethod testMethod : getTestClass().getAnnotatedMethods(ArchTest.class)) {
            boolean ignore = elementShouldBeIgnored(testMethod.getMethod());
            result.add(new ArchTestMethodExecution(getTestClass().getJavaClass(), testMethod.getMethod(), ignore));
        }
        return result;
    }

    @Override
    protected Description describeChild(ArchTestExecution child) {
        return child.describeSelf();
    }

    @Override
    protected void runChild(ArchTestExecution child, RunNotifier notifier) {
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
        public void handleFieldDeclaration(Field field, Class<?> fieldOwner, boolean ignore) {
            executions.add(new ArchRuleExecution(fieldOwner, field, ignore));
        }

        @Override
        public void handleMethodDeclaration(Method method, Class<?> methodOwner, boolean ignore) {
            executions.add(new ArchTestMethodExecution(methodOwner, method, ignore));
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
    }
}
