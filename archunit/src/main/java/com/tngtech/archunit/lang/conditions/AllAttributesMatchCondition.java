package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;

import static com.tngtech.archunit.lang.conditions.ArchConditions.containOnlyElementsThat;

abstract class AllAttributesMatchCondition<T> extends ArchCondition<JavaClass> {
    private final ArchCondition<T> condition;

    AllAttributesMatchCondition(String description, ArchCondition<T> condition) {
        super(description);
        this.condition = condition;
    }

    @Override
    public final void check(JavaClass item, ConditionEvents events) {
        containOnlyElementsThat(condition).check(relevantAttributes(item), events);
    }

    abstract Collection<T> relevantAttributes(JavaClass item);

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{condition=" + condition + "}";
    }
}
