package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.AbstractArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;

import static com.tngtech.archunit.lang.conditions.ArchConditions.containsAny;

abstract class ClassMatchesAnyCondition<T> extends AbstractArchCondition<JavaClass> {
    private final AbstractArchCondition<T> condition;

    ClassMatchesAnyCondition(AbstractArchCondition<T> condition) {
        this.condition = condition;
    }

    @Override
    public void check(JavaClass item, ConditionEvents events) {
        containsAny(condition).check(relevantAttributes(item), events);
    }

    abstract Collection<T> relevantAttributes(JavaClass item);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{condition=" + condition + "}";
    }
}
