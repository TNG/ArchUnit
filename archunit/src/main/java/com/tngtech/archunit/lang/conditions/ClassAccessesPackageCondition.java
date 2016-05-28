package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.google.common.collect.Sets;
import com.tngtech.archunit.core.JavaAccess;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.AbstractArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;

class ClassAccessesPackageCondition extends ClassMatchesAnyCondition<JavaAccess<?>> {
    ClassAccessesPackageCondition(String packageIdentifier) {
        super(new PackageAccessCondition(packageIdentifier));
    }

    @Override
    Collection<JavaAccess<?>> relevantAttributes(JavaClass item) {
        return Sets.union(item.getMethodCalls(), item.getFieldAccesses());
    }

    static class PackageAccessCondition extends AbstractArchCondition<JavaAccess<?>> {
        private final PackageMatcher packageMatcher;

        PackageAccessCondition(String packageIdentifier) {
            packageMatcher = PackageMatcher.of(packageIdentifier);
        }

        @Override
        public void check(JavaAccess<?> item, ConditionEvents events) {
            boolean matches = packageMatcher.matches(item.getTarget().getOwner().getPackage());
            events.add(new ConditionEvent(matches, item.getDescription()));
        }
    }
}
