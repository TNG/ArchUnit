package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.tngtech.archunit.core.JavaAccess;
import com.tngtech.archunit.core.JavaClass;

class ClassAccessesAnyPackageCondition extends ClassMatchesAnyCondition<JavaAccess<?>> {
    ClassAccessesAnyPackageCondition(String[] packageIdentifiers) {
        super(AccessPackageCondition.forAccessTarget().matching(packageIdentifiers));
    }

    @Override
    Collection<JavaAccess<?>> relevantAttributes(JavaClass item) {
        return item.getAccessesFromSelf();
    }
}
