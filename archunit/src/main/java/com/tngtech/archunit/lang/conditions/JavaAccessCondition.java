package com.tngtech.archunit.lang.conditions;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.JavaAccess;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

class JavaAccessCondition extends ArchCondition<JavaAccess<?>> {
    private final DescribedPredicate<? super JavaAccess<?>> predicate;

    JavaAccessCondition(DescribedPredicate<? super JavaAccess<?>> predicate) {
        super(predicate.getDescription());
        this.predicate = predicate;
    }

    @Override
    public void check(JavaAccess<?> item, ConditionEvents events) {
        events.add(new SimpleConditionEvent<>(item, predicate.apply(item), item.getDescription()));
    }
}
