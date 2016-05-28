package com.tngtech.archunit.lang;

import com.tngtech.archunit.core.HasDescription;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A specification of {@link ArchRule} where the set of classes is known at the time the
 * rule is defined.<br><p>
 * <b>Example</b>
 * <pre><code>
 * all(classes).should("do something").assertedBy(conditionThatDetectsThis)
 * </code></pre></p>
 *
 * @param <T> The type of objects the rule applies to (e.g. {@link com.tngtech.archunit.core.JavaClass})
 * @see ArchRule
 * @see OpenArchRule
 */
public final class ClosedArchRule<T> extends ArchRule<T> {
    private final Iterable<T> objectsToTest;

    private ClosedArchRule(Iterable<T> objectsToTest, String text, ArchCondition<T> condition) {
        super(text, finish(condition, objectsToTest));
        this.objectsToTest = objectsToTest;
    }

    private static <T> ArchCondition<T> finish(ArchCondition<T> condition, Iterable<T> objectsToTest) {
        condition.objectsToTest = objectsToTest;
        return condition;
    }

    ConditionEvents evaluate() {
        ConditionEvents events = new ConditionEvents();
        super.evaluate(objectsToTest, events);
        return events;
    }

    public static class ClosedDescribable<TYPE, ITERABLE extends Iterable<TYPE> & HasDescription> {
        private final ITERABLE describedCollection;

        ClosedDescribable(ITERABLE describedCollection) {
            this.describedCollection = describedCollection;
        }

        public Assertion<TYPE, ITERABLE> should(String ruleTextSuffix) {
            String completeRuleText = String.format("%s should %s", describedCollection.getDescription(), ruleTextSuffix);
            return new Assertion<>(describedCollection, completeRuleText);
        }
    }

    public static class Assertion<T, S extends Iterable<T>> {
        private final Iterable<T> objectsToTest;
        private Priority priority = Priority.MEDIUM;

        private final String ruleText;

        Assertion(S objectsToTest, String ruleText) {
            this.objectsToTest = objectsToTest;
            this.ruleText = ruleText;
        }

        public Assertion<T, S> withPriority(Priority priority) {
            this.priority = checkNotNull(priority);
            return this;
        }

        public void assertedBy(ArchCondition<T> condition) {
            ClosedArchRule<?> rule = new ClosedArchRule<>(objectsToTest, ruleText, condition);
            ArchRuleAssertion.from(rule).assertNoViolations(priority);
        }
    }
}