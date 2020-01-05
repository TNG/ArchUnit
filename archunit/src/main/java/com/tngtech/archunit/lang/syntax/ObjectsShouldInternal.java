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
package com.tngtech.archunit.lang.syntax;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.Function.Functions;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClasses;
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

    ObjectsShouldInternal(ClassesTransformer<? extends T> classesTransformer,
            Priority priority,
            Function<ArchCondition<T>, ArchCondition<T>> prepareCondition) {
        this(classesTransformer, priority, new ConditionAggregator<T>(), prepareCondition);
    }

    ObjectsShouldInternal(ClassesTransformer<? extends T> classesTransformer,
            Priority priority,
            ArchCondition<T> condition,
            Function<ArchCondition<T>, ArchCondition<T>> prepareCondition) {
        this(classesTransformer, priority, new ConditionAggregator<>(condition), prepareCondition);
    }

    ObjectsShouldInternal(ClassesTransformer<? extends T> classesTransformer,
            Priority priority,
            ConditionAggregator<T> conditionAggregator,
            Function<ArchCondition<T>, ArchCondition<T>> prepareCondition) {
        this.conditionAggregator = conditionAggregator;
        this.classesTransformer = getTyped(classesTransformer);
        this.priority = priority;
        this.prepareCondition = prepareCondition;
    }

    // A ClassesTransformer<? extends T> is in particular a ClassesTransformer<T> (covariance)
    @SuppressWarnings("unchecked")
    private ClassesTransformer<T> getTyped(ClassesTransformer<? extends T> classesTransformer) {
        return (ClassesTransformer<T>) classesTransformer;
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
    public ArchRule because(String reason) {
        return ArchRule.Factory.withBecause(this, reason);
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
        private final AddMode<T> addMode;

        ConditionAggregator() {
            this(Optional.<ArchCondition<T>>absent(), AddMode.<T>and());
        }

        ConditionAggregator(ArchCondition<T> condition) {
            this(Optional.of(condition), AddMode.<T>and());
        }

        private ConditionAggregator(Optional<ArchCondition<T>> condition, AddMode<T> addMode) {
            this.condition = condition;
            this.addMode = addMode;
        }

        ArchCondition<T> getCondition() {
            checkState(condition.isPresent(),
                    "No condition was added to this rule, this is most likely a bug within the syntax");
            return condition.get();
        }

        ArchCondition<T> add(ArchCondition<? super T> other) {
            return addMode.apply(condition, other);
        }

        ConditionAggregator<T> thatORsWith(Function<ArchCondition<T>, ArchCondition<T>> prepareCondition) {
            return new ConditionAggregator<>(condition, AddMode.or(prepareCondition));
        }

        ConditionAggregator<T> thatANDsWith(Function<ArchCondition<T>, ArchCondition<T>> prepareCondition) {
            return new ConditionAggregator<>(condition, AddMode.and(prepareCondition));
        }
    }

    private abstract static class AddMode<T> {
        static <T> AddMode<T> and() {
            return and(Functions.<ArchCondition<T>>identity());
        }

        static <T> AddMode<T> and(final Function<ArchCondition<T>, ArchCondition<T>> prepareCondition) {
            return new AddMode<T>() {
                @Override
                ArchCondition<T> apply(Optional<ArchCondition<T>> first, ArchCondition<? super T> other) {
                    ArchCondition<T> second = prepareCondition.apply(other.<T>forSubType());
                    return first.isPresent() ? first.get().and(second) : second;
                }
            };
        }

        static <T> AddMode<T> or(final Function<ArchCondition<T>, ArchCondition<T>> prepareCondition) {
            return new AddMode<T>() {
                @Override
                ArchCondition<T> apply(Optional<ArchCondition<T>> first, ArchCondition<? super T> other) {
                    ArchCondition<T> second = prepareCondition.apply(other.<T>forSubType());
                    return first.isPresent() ? first.get().or(second) : second;
                }
            };
        }

        abstract ArchCondition<T> apply(Optional<ArchCondition<T>> first, ArchCondition<? super T> other);
    }

    static <T> Function<ArchCondition<T>, ArchCondition<T>> prependDescription(final String prefix) {
        return new Function<ArchCondition<T>, ArchCondition<T>>() {
            @Override
            public ArchCondition<T> apply(ArchCondition<T> input) {
                return input.as(prefix + " " + input.getDescription());
            }
        };
    }
}
