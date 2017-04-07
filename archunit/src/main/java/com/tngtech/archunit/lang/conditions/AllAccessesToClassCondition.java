package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.google.common.base.Joiner;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.JavaAccess;
import com.tngtech.archunit.core.JavaClass;

class AllAccessesToClassCondition extends AllAttributesMatchCondition<JavaAccess<?>> {
    AllAccessesToClassCondition(String prefix, DescribedPredicate<JavaAccess<?>> predicate) {
        super(Joiner.on(" ").join(prefix, predicate.getDescription()), new JavaAccessCondition(predicate));
    }

    @Override
    Collection<JavaAccess<?>> relevantAttributes(JavaClass item) {
        return item.getAccessesToSelf();
    }
}
