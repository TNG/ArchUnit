package com.tngtech.archunit.lang;

import com.tngtech.archunit.core.HasDescription;

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
        private final Priority priority;

        ClosedDescribable(ITERABLE describedCollection, Priority priority) {
            this.describedCollection = describedCollection;
            this.priority = priority;
        }

        public void should(ArchCondition<TYPE> condition) {
            String completeRuleText = String.format("%s should %s", describedCollection.getDescription(), condition.getDescription());
            ClosedArchRule<?> rule = new ClosedArchRule<>(describedCollection, completeRuleText, condition);
            ArchRuleAssertion.from(rule).assertNoViolations(priority);
        }
    }
}