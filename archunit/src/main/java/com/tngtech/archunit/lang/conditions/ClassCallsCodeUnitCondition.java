package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.core.JavaClass;

class ClassCallsCodeUnitCondition extends AnyAttributeMatchesCondition<JavaCall<?>> {
    ClassCallsCodeUnitCondition(DescribedPredicate<? super JavaCall<?>> predicate) {
        super(new CodeUnitCallCondition(predicate));
    }

    @Override
    Collection<JavaCall<?>> relevantAttributes(JavaClass item) {
        return item.getCallsFromSelf();
    }
}
