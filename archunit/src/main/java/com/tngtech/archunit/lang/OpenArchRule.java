package com.tngtech.archunit.lang;

import com.tngtech.archunit.core.HasDescription;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.lang.ClosedArchRule.ClosedDescribable;

/**
 * A specification of {@link ArchRule} where the set of classes is not known at the time the
 * rule is defined, or where a rule is supposed to be applied to multiple sets of objects.<br><p>
 * <b>Example</b>
 * <pre><code>
 * all(services).should("not access the ui").assertedBy(conditionThatDetectsThis)
 * </code></pre></p>
 *
 * @param <T> The type of objects the rule applies to (e.g. {@link com.tngtech.archunit.core.JavaClass})
 * @see ArchRule
 * @see ClosedArchRule
 */
public final class OpenArchRule<T> extends ArchRule<T> {
    private final Priority priority;
    private final InputTransformer<T> inputTransformer;

    private OpenArchRule(OpenDescribable<T> describable, ArchCondition<T> condition) {
        super(condition.getDescription(), condition);
        this.priority = describable.priority;
        this.inputTransformer = describable.inputTransformer;
    }

    public <U extends Iterable<T> & HasDescription> void check(JavaClasses classes) {
        all(inputTransformer.transform(classes)).should(condition);
    }

    private <TYPE, ITERABLE extends Iterable<TYPE> & HasDescription>
    ClosedDescribable<TYPE, ITERABLE> all(ITERABLE iterable) {
        return new ClosedDescribable<>(iterable, priority);
    }

    public static class OpenDescribable<TYPE> {
        private final InputTransformer<TYPE> inputTransformer;
        private final Priority priority;

        OpenDescribable(InputTransformer<TYPE> inputTransformer, Priority priority) {
            this.inputTransformer = inputTransformer;
            this.priority = priority;
        }

        public OpenArchRule<TYPE> should(ArchCondition<TYPE> condition) {
            return new OpenArchRule<>(this, condition);
        }
    }
}