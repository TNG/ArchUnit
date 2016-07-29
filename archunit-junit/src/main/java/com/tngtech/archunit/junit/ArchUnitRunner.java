package com.tngtech.archunit.junit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.lang.OpenArchRule;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;

/**
 * Evaluates {@link OpenArchRule}s against the classes inside of the packages specified via
 * <p/>
 * {@link AnalyseClasses @AnalyseClasses} on the annotated test class.
 * <p>
 * NOTE: The runner demands {@link AnalyseClasses @AnalyseClasses} to be present on the respective test class.
 * </p>
 * Example
 * <pre><code>
 *    {@literal @}RunWith(ArchUnitRunner.class)
 *    {@literal @}AnalyseClasses(packages = "com.example")
 *    public class SomeArchTest {
 *        {@literal @}ArchTest
 *        public final ArchRule&lt;JavaClass&gt; some_rule = rule(all(JavaClass.class))
 *                .should("satisfy something special")
 *                .assertedBy(mySpecificCondition);
 *    }
 * </code></pre>
 */
public class ArchUnitRunner extends ParentRunner<ArchTestExecution> {
    private SharedCache cache = new SharedCache(); // NOTE: We want to change this in tests -> no static reference

    public ArchUnitRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
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
            return getArchRules(ruleField).asTestExecutions();
        }
        return Collections.<ArchTestExecution>singleton(new ArchRuleExecution(getTestClass().getJavaClass(), ruleField.getField()));
    }

    private ArchRules<?> getArchRules(FrameworkField ruleField) {
        ArchTestExecution.validate(ruleField.getField());
        try {
            return (ArchRules<?>) ruleField.get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private Collection<ArchTestExecution> findArchRuleMethods() {
        List<ArchTestExecution> result = new ArrayList<>();
        for (FrameworkMethod testMethod : getTestClass().getAnnotatedMethods(ArchTest.class)) {
            result.add(new ArchTestMethodExecution(getTestClass().getJavaClass(), testMethod.getMethod()));
        }
        return result;
    }

    @Override
    protected Description describeChild(ArchTestExecution child) {
        return child.describeSelf();
    }

    @Override
    protected void runChild(ArchTestExecution child, RunNotifier notifier) {
        notifier.fireTestStarted(describeChild(child));
        JavaClasses classes = cache.get().getClassesToAnalyseFor(getTestClass().getJavaClass());
        child.evaluateOn(classes).notify(notifier);
        notifier.fireTestFinished(describeChild(child));
    }

    static class SharedCache {
        private static final ClassCache cache = new ClassCache();

        ClassCache get() {
            return cache;
        }
    }
}
