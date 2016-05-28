package com.tngtech.archunit.lang.conditions;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;

class ClassResidesInCondition extends ArchCondition<JavaClass> {
    private final String packageIdentifier;

    ClassResidesInCondition(String packageIdentifier) {
        this.packageIdentifier = packageIdentifier;
    }

    @Override
    public void check(final JavaClass item, ConditionEvents events) {
        boolean matches = PackageMatcher.of(packageIdentifier).matches(item.getPackage());
        String message = String.format("Class %s does not reside in a package that matches '%s'", item.getName(), packageIdentifier);
        events.add(new ConditionEvent(matches, message));
    }
}
