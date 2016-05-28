package com.tngtech.archunit.lang;

import java.util.Collection;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class AbstractArchCondition<T> {
    Iterable<T> objectsToTest;

    public abstract void check(T item, ConditionEvents events);

    protected final Iterable<T> allObjectsToTest() {
        return checkNotNull(objectsToTest, "Objects to test were never set, this is most likely a bug");
    }

    public AbstractArchCondition<T> and(AbstractArchCondition<T> condition) {
        return new AndCondition<>(this, condition);
    }

    private static class AndCondition<T> extends AbstractArchCondition<T> {
        private final Collection<AbstractArchCondition<T>> conditions;

        private AndCondition(AbstractArchCondition<T> first, AbstractArchCondition<T> second) {
            this(ImmutableList.of(first, second));
        }

        private AndCondition(Collection<AbstractArchCondition<T>> conditions) {
            this.conditions = conditions;
        }

        @Override
        public void check(T item, ConditionEvents events) {
            for (AbstractArchCondition<T> condition : conditions) {
                condition.check(item, events);
            }
        }
    }
}
