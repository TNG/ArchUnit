package com.tngtech.archunit.lang.syntax;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.Priority;

import static com.google.common.base.Preconditions.checkState;

class ObjectsShouldInternal<T> implements ArchRule {
    private final Supplier<ArchRule> finishedRule = Suppliers.memoize(new FinishedRule());

    final ConditionAggregator<T> conditionAggregator;
    final ClassesTransformer<T> classesTransformer;
    final Priority priority;
    final Function<ArchCondition<T>, ArchCondition<T>> prepareCondition;

    ObjectsShouldInternal(ClassesTransformer<T> classesTransformer,
                          Priority priority,
                          Function<ArchCondition<T>, ArchCondition<T>> prepareCondition) {
        this(classesTransformer, priority, new ConditionAggregator<T>(), prepareCondition);
    }

    ObjectsShouldInternal(ClassesTransformer<T> classesTransformer,
                          Priority priority,
                          ArchCondition<T> condition,
                          Function<ArchCondition<T>, ArchCondition<T>> prepareCondition) {
        this(classesTransformer, priority, new ConditionAggregator<>(condition), prepareCondition);
    }

    private ObjectsShouldInternal(ClassesTransformer<T> classesTransformer,
                                  Priority priority,
                                  ConditionAggregator<T> conditionAggregator,
                                  Function<ArchCondition<T>, ArchCondition<T>> prepareCondition) {
        this.conditionAggregator = conditionAggregator;
        this.classesTransformer = classesTransformer;
        this.priority = priority;
        this.prepareCondition = prepareCondition;
    }

    @Override
    public String getDescription() {
        return finishedRule.get().getDescription();
    }

    @Override
    public EvaluationResult evaluate(JavaClasses classes) {
        return finishedRule.get().evaluate(classes);
    }

    @Override
    public void check(JavaClasses classes) {
        finishedRule.get().check(classes);
    }

    @Override
    public ArchRule as(String newDescription) {
        return finishedRule.get().as(newDescription);
    }

    @Override
    public String toString() {
        return finishedRule.get().getDescription();
    }

    private class FinishedRule implements Supplier<ArchRule> {
        @Override
        public ArchRule get() {
            return ArchRule.Factory.create(classesTransformer, createCondition(), priority);
        }

        private ArchCondition<T> createCondition() {
            return prepareCondition.apply(conditionAggregator.getCondition());
        }
    }

    static class ConditionAggregator<T> {
        private final Optional<ArchCondition<T>> condition;

        ConditionAggregator(ArchCondition<T> condition) {
            this(Optional.of(condition));
        }

        ConditionAggregator() {
            this(Optional.<ArchCondition<T>>absent());
        }

        private ConditionAggregator(Optional<ArchCondition<T>> condition) {
            this.condition = condition;
        }

        ArchCondition<T> getCondition() {
            checkState(condition.isPresent(),
                    "No condition was added to this rule, this is most likely a bug within the syntax");
            return condition.get();
        }

        ArchCondition<T> and(ArchCondition<? super T> other) {
            return condition.isPresent() ? condition.get().and(other) : other.<T>forSubType();
        }

        ArchCondition<T> or(ArchCondition<? super T> other) {
            return condition.isPresent() ? condition.get().or(other) : other.<T>forSubType();
        }
    }
}
