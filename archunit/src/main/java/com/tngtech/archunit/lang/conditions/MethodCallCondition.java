package com.tngtech.archunit.lang.conditions;

import com.google.common.base.Predicate;
import com.tngtech.archunit.core.JavaMethodLikeCall;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;

class MethodCallCondition extends ArchCondition<JavaMethodLikeCall<?>> {
    private final Predicate<JavaMethodLikeCall<?>> callIdentifier;

    MethodCallCondition(Predicate<JavaMethodLikeCall<?>> callIdentifier) {
        this.callIdentifier = callIdentifier;
    }

    @Override
    public void check(JavaMethodLikeCall<?> item, ConditionEvents events) {
        events.add(new ConditionEvent(callIdentifier.apply(item), item.getDescription()));
    }
}
