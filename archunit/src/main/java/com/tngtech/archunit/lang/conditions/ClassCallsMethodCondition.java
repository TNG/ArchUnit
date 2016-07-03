package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.tngtech.archunit.core.FluentPredicate;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.core.JavaClass;

class ClassCallsMethodCondition extends ClassMatchesAnyCondition<JavaCall<?>> {
    public ClassCallsMethodCondition(FluentPredicate<JavaCall<?>> predicate) {
        super(new MethodCallCondition(predicate));
    }

    @Override
    Collection<JavaCall<?>> relevantAttributes(JavaClass item) {
        return item.getCallsFromSelf();
    }
}
