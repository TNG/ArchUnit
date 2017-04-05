package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.JavaAccess;
import com.tngtech.archunit.core.JavaClass;

class ClassAccessesTargetCondition extends AnyAttributeMatchesCondition<JavaAccess<?>> {
    ClassAccessesTargetCondition(DescribedPredicate<? super JavaAccess<?>> predicate) {
        super(new AccessTargetCondition(predicate));
    }

    @Override
    Collection<JavaAccess<?>> relevantAttributes(JavaClass item) {
        return item.getAccessesFromSelf();
    }
}
