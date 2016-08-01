package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.google.common.base.Joiner;
import com.tngtech.archunit.core.JavaAccess;
import com.tngtech.archunit.core.JavaClass;

class ClassAccessesAnyPackageCondition extends AnyAttributeMatchesCondition<JavaAccess<?>> {
    ClassAccessesAnyPackageCondition(String[] packageIdentifiers) {
        super(String.format("access classes that reside in any package ['%s']", Joiner.on("', '").join(packageIdentifiers)),
                AccessPackageCondition.forAccessTarget().matching(packageIdentifiers));
    }

    @Override
    Collection<JavaAccess<?>> relevantAttributes(JavaClass item) {
        return item.getAccessesFromSelf();
    }
}
