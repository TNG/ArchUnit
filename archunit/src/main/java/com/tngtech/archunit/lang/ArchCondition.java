package com.tngtech.archunit.lang;

import java.util.Collection;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class ArchCondition<T> {
    Iterable<T> objectsToTest;

    public abstract void check(T item, ConditionEvents events);

    protected final Iterable<T> allObjectsToTest() {
        return checkNotNull(objectsToTest, "Objects to test were never set, this is most likely a bug");
    }

    public ArchCondition<T> and(ArchCondition<T> condition) {
        return new AndCondition<>(this, condition);
    }

    private static class AndCondition<T> extends ArchCondition<T> {
        private final Collection<ArchCondition<T>> conditions;

        private AndCondition(ArchCondition<T> first, ArchCondition<T> second) {
            this(ImmutableList.of(first, second));
        }

        private AndCondition(Collection<ArchCondition<T>> conditions) {
            this.conditions = conditions;
        }

        @Override
        public void check(T item, ConditionEvents events) {
            for (ArchCondition<T> condition : conditions) {
                condition.check(item, events);
            }
        }
    }
}
