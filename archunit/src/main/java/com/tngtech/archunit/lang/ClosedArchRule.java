package com.tngtech.archunit.lang;

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

    ClosedArchRule(Iterable<T> objectsToTest, String text, ArchCondition<T> condition) {
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
}