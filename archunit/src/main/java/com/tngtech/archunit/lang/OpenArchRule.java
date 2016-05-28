package com.tngtech.archunit.lang;

import com.tngtech.archunit.core.HasDescription;
import com.tngtech.archunit.core.JavaClasses;

import static com.google.common.base.Preconditions.checkNotNull;

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

    private OpenArchRule(Creator<T> creator, ArchCondition<T> condition) {
        super(creator.ruleTextSuffix, condition);
        this.priority = creator.priority;
        this.inputTransformer = creator.inputTransformer;
    }

    public <U extends Iterable<T> & HasDescription> void check(JavaClasses classes) {
        all(inputTransformer.<U>transform(classes)).should(text).withPriority(priority).assertedBy(condition);
    }

    public static class OpenDescribable<TYPE> {
        private final InputTransformer<TYPE> inputTransformer;

        OpenDescribable(InputTransformer<TYPE> inputTransformer) {
            this.inputTransformer = inputTransformer;
        }

        public Creator<TYPE> should(String ruleText) {
            return new Creator<>(inputTransformer, ruleText);
        }
    }

    public static class Creator<T> {
        private final InputTransformer<T> inputTransformer;
        private final String ruleTextSuffix;
        private Priority priority = Priority.MEDIUM;

        Creator(InputTransformer<T> inputTransformer, String ruleTextSuffix) {
            this.inputTransformer = inputTransformer;
            this.ruleTextSuffix = ruleTextSuffix;
        }

        public Creator<T> withPriority(Priority priority) {
            this.priority = checkNotNull(priority);
            return this;
        }

        public OpenArchRule<T> assertedBy(ArchCondition<T> condition) {
            return new OpenArchRule<>(this, condition);
        }
    }
}