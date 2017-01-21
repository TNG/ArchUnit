package com.tngtech.archunit.lang.syntax;

import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ClassesTransformer;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.Priority;

import static com.google.common.base.Preconditions.checkState;

class ObjectsShouldInternal<T> implements ArchRule {
    private final Supplier<ArchRule> finishedRule = Suppliers.memoize(new FinishedRule());

    final List<ArchCondition<T>> conditions;
    final ClassesTransformer<T> classesTransformer;
    final Priority priority;
    final Function<ArchCondition<T>, ArchCondition<T>> prepareCondition;

    ObjectsShouldInternal(ClassesTransformer<T> classesTransformer, Priority priority, List<ArchCondition<T>> conditions, Function<ArchCondition<T>, ArchCondition<T>> prepareCondition) {
        this.conditions = conditions;
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

    private class FinishedRule implements Supplier<ArchRule> {
        @Override
        public ArchRule get() {
            return ArchRule.Factory.create(classesTransformer, createCondition(), priority);
        }

        private ArchCondition<T> createCondition() {
            checkState(conditions.size() >= 1,
                    "No condition was specified for this rule, so this rule would always be satisfied");
            LinkedList<ArchCondition<T>> combine = new LinkedList<>(conditions);
            ArchCondition<T> result = combine.pollFirst();
            for (ArchCondition<T> condition : combine) {
                result = result.and(condition);
            }
            return prepareCondition.apply(result);
        }
    }
}
