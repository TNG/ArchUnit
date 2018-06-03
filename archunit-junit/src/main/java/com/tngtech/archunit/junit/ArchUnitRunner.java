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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.tngtech.archunit.Internal;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.junit.ArchTestExecution.elementShouldBeIgnored;

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
        if (ruleField.getType() == ArchRules.class) {
            boolean ignore = elementShouldBeIgnored(ruleField.getField());
            return getArchRules(ruleField).asTestExecutions(ignore);
        }
        return Collections.<ArchTestExecution>singleton(new ArchRuleExecution(getTestClass().getJavaClass(), ruleField.getField(), false));
    }

    private ArchRules getArchRules(FrameworkField ruleField) {
        ArchTestExecution.validatePublicStatic(ruleField.getField());
        try {
            return (ArchRules) ruleField.get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Collection<ArchTestExecution> findArchRuleMethods() {
        List<ArchTestExecution> result = new ArrayList<>();
        for (FrameworkMethod testMethod : getTestClass().getAnnotatedMethods(ArchTest.class)) {
            result.add(new ArchTestMethodExecution(getTestClass().getJavaClass(), testMethod.getMethod(), false));
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
            JavaClasses classes = cache.get().getClassesToAnalyzeFor(getTestClass().getJavaClass());
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
}
