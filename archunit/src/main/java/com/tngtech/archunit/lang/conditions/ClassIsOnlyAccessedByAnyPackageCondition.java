package com.tngtech.archunit.lang.conditions;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;

import static com.tngtech.archunit.lang.conditions.ArchConditions.containsOnly;

class ClassIsOnlyAccessedByAnyPackageCondition extends ArchCondition<JavaClass> {
    private final AccessPackageCondition condition;

    ClassIsOnlyAccessedByAnyPackageCondition(String[] packageIdentifiers) {
        condition = AccessPackageCondition.forAccessOrigin().matching(packageIdentifiers);
    }

    @Override
    public void check(JavaClass item, ConditionEvents events) {
        containsOnly(condition).check(item.getAccessesToSelf(), events);
    }
}
