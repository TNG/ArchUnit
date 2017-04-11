package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.google.common.base.Joiner;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaClass;

class AnyAccessFromClassCondition extends AnyAttributeMatchesCondition<JavaAccess<?>> {
    AnyAccessFromClassCondition(String prefix, DescribedPredicate<? super JavaAccess<?>> predicate) {
        super(Joiner.on(" ").join(prefix, predicate.getDescription()), new JavaAccessCondition(predicate));
    }

    @Override
    Collection<JavaAccess<?>> relevantAttributes(JavaClass item) {
        return item.getAccessesFromSelf();
    }
}
