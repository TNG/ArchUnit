package com.tngtech.archunit.lang.conditions;

import com.google.common.base.Predicate;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;

class MethodCallCondition extends ArchCondition<JavaCall<?>> {
    private final Predicate<JavaCall<?>> callIdentifier;

    MethodCallCondition(Predicate<JavaCall<?>> callIdentifier) {
        this.callIdentifier = callIdentifier;
    }

    @Override
    public void check(JavaCall<?> item, ConditionEvents events) {
        events.add(new ConditionEvent(callIdentifier.apply(item), item.getDescription()));
    }
}
