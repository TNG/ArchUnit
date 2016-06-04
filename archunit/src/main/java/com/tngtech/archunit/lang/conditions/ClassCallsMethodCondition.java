package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.google.common.base.Predicate;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.core.JavaClass;

class ClassCallsMethodCondition extends ClassMatchesAnyCondition<JavaCall<?>> {
    public ClassCallsMethodCondition(Predicate<JavaCall<?>> predicate) {
        super(new MethodCallCondition(predicate));
    }

    @Override
    Collection<JavaCall<?>> relevantAttributes(JavaClass item) {
        return item.getCallsFromSelf();
    }
}
