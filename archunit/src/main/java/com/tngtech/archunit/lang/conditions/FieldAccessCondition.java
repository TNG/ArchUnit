package com.tngtech.archunit.lang.conditions;

import java.util.EnumSet;
import java.util.Set;

import com.tngtech.archunit.core.FluentPredicate;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.core.JavaFieldAccess.AccessType;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;

import static com.tngtech.archunit.core.JavaFieldAccess.AccessType.GET;
import static com.tngtech.archunit.core.JavaFieldAccess.AccessType.SET;
import static com.tngtech.archunit.core.JavaFieldAccess.getDescriptionTemplateFor;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.hasAccessType;
import static java.util.Collections.singleton;

class FieldAccessCondition extends ArchCondition<JavaFieldAccess> {
    private final FluentPredicate<JavaFieldAccess> fieldAccessIdentifier;
    private final String descriptionTemplate;

    FieldAccessCondition(FluentPredicate<JavaFieldAccess> fieldAccessIdentifier) {
        this(fieldAccessIdentifier, EnumSet.allOf(AccessType.class));
    }

    FieldAccessCondition(FluentPredicate<JavaFieldAccess> fieldAccessIdentifier, Set<AccessType> accessTypes) {
        this.fieldAccessIdentifier = fieldAccessIdentifier;
        this.descriptionTemplate = getDescriptionTemplateFor(accessTypes);
    }

    @Override
    public void check(JavaFieldAccess item, ConditionEvents events) {
        String message = item.getDescriptionWithTemplate(descriptionTemplate);
        events.add(new ConditionEvent(fieldAccessIdentifier.apply(item), message));
    }

    static class FieldGetAccessCondition extends FieldAccessCondition {
        FieldGetAccessCondition(FluentPredicate<JavaFieldAccess> predicate) {
            super(predicate.and(hasAccessType(GET)), singleton(GET));
        }
    }

    static class FieldSetAccessCondition extends FieldAccessCondition {
        FieldSetAccessCondition(FluentPredicate<JavaFieldAccess> predicate) {
            super(predicate.and(hasAccessType(SET)), singleton(SET));
        }
    }
}
