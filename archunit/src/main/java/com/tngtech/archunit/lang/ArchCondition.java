package com.tngtech.archunit.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class ArchCondition<T> {
    private String description;

    public ArchCondition(String description) {
        this.description = checkNotNull(description);
    }

    /**
     * Can be used to prepare this condition with respect to the collection of all objects the condition
     * will be tested against.
     *
     * @param allObjectsToTest All objects that {@link #check(Object, ConditionEvents)} will be called against
     */
    public void init(Iterable<T> allObjectsToTest) {
    }

    public abstract void check(T item, ConditionEvents events);

    public ArchCondition<T> and(ArchCondition<T> condition) {
        return new AndCondition<>(this, condition);
    }

    public String getDescription() {
        return description;
    }

    public ArchCondition<T> as(String description, Object... args) {
        return new ArchCondition<T>(String.format(description, args)) {
            @Override
            public void init(Iterable<T> allObjectsToTest) {
                ArchCondition.this.init(allObjectsToTest);
            }

            @Override
            public void check(T item, ConditionEvents events) {
                ArchCondition.this.check(item, events);
            }
        };
    }

    private static class AndCondition<T> extends ArchCondition<T> {
        private final Collection<ArchCondition<T>> conditions;

        private AndCondition(ArchCondition<T> first, ArchCondition<T> second) {
            this(ImmutableList.of(first, second));
        }

        private AndCondition(Collection<ArchCondition<T>> conditions) {
            super(joinDescriptionsOf(conditions));
            this.conditions = conditions;
        }

        private static <T> String joinDescriptionsOf(Collection<ArchCondition<T>> conditions) {
            List<String> descriptions = new ArrayList<>();
            for (ArchCondition<T> condition : conditions) {
                descriptions.add(condition.getDescription());
            }
            return Joiner.on(" and ").join(descriptions);
        }

        @Override
        public void init(Iterable<T> allObjectsToTest) {
            for (ArchCondition<T> condition : conditions) {
                condition.init(allObjectsToTest);
            }
        }

        @Override
        public void check(T item, ConditionEvents events) {
            for (ArchCondition<T> condition : conditions) {
                condition.check(item, events);
            }
        }
    }
}
