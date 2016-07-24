package com.tngtech.archunit.lang.conditions;

import com.google.common.base.Joiner;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;

import static com.tngtech.archunit.lang.conditions.ArchConditions.containOnlyElementsThat;

class ClassIsOnlyAccessedByAnyPackageCondition extends ArchCondition<JavaClass> {
    private final AccessPackageCondition condition;

    ClassIsOnlyAccessedByAnyPackageCondition(String[] packageIdentifiers) {
        super(String.format("only be accessed by classes that reside in any package ['%s']",
                Joiner.on("', '").join(packageIdentifiers)));
        condition = AccessPackageCondition.forAccessOrigin().matching(packageIdentifiers);
    }

    @Override
    public void check(JavaClass item, ConditionEvents events) {
        containOnlyElementsThat(condition).check(item.getAccessesToSelf(), events);
    }
}
