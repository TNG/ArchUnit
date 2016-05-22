package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.google.common.base.Predicate;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaMethodLikeCall;

class ClassCallsMethodCondition extends ClassMatchesAnyCondition<JavaMethodLikeCall<?>> {
    public ClassCallsMethodCondition(Predicate<JavaMethodLikeCall<?>> predicate) {
        super(new MethodCallCondition(predicate));
    }

    @Override
    Collection<JavaMethodLikeCall<?>> relevantAttributes(JavaClass item) {
        return item.getMethodCalls();
    }
}
