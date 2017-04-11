package com.tngtech.archunit.lang.conditions;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

class AccessTargetCondition extends ArchCondition<JavaAccess<?>> {
    private final DescribedPredicate<? super JavaAccess<?>> callIdentifier;

    AccessTargetCondition(DescribedPredicate<? super JavaAccess<?>> callIdentifier) {
        super("access target where " + callIdentifier.getDescription());
        this.callIdentifier = callIdentifier;
    }

    @Override
    public void check(JavaAccess<?> item, ConditionEvents events) {
        events.add(new SimpleConditionEvent<>(item, callIdentifier.apply(item), item.getDescription()));
    }
}
