package com.tngtech.archunit.lang.conditions;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

class CodeUnitCallCondition extends ArchCondition<JavaCall<?>> {
    private final DescribedPredicate<? super JavaCall<?>> callIdentifier;

    CodeUnitCallCondition(DescribedPredicate<? super JavaCall<?>> callIdentifier) {
        super("call code unit where " + callIdentifier.getDescription());
        this.callIdentifier = callIdentifier;
    }

    @Override
    public void check(JavaCall<?> item, ConditionEvents events) {
        events.add(new SimpleConditionEvent<>(item, callIdentifier.apply(item), item.getDescription()));
    }
}
