package com.tngtech.archunit.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.DescribedIterable;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class ArchCondition<T> {
    DescribedIterable<T> objectsToTest;
    private String description;

    public ArchCondition(String description) {
        this(null, description);
    }

    private ArchCondition(DescribedIterable<T> objectsToTest, String description) {
        this.objectsToTest = objectsToTest;
        this.description = checkNotNull(description);
    }

    void check(ConditionEvents events) {
        for (T object : objectsToTest) {
            check(object, events);
        }
    }

    public abstract void check(T item, ConditionEvents events);

    protected final Iterable<T> allObjectsToTest() {
        return checkNotNull(objectsToTest, "Objects to test were never set, this is most likely a bug");
    }

    public ArchCondition<T> and(ArchCondition<T> condition) {
        return new AndCondition<>(this, condition);
    }

    public String getDescription() {
        return description;
    }

    public ArchCondition<T> as(String description, Object... args) {
        return new ArchCondition<T>(objectsToTest, String.format(description, args)) {
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
        public void check(T item, ConditionEvents events) {
            for (ArchCondition<T> condition : conditions) {
                condition.check(item, events);
            }
        }
    }
}
