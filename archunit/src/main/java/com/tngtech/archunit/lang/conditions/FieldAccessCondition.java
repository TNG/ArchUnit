package com.tngtech.archunit.lang.conditions;

import java.util.EnumSet;
import java.util.Set;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.core.JavaFieldAccess.AccessType;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;

import static com.tngtech.archunit.core.JavaFieldAccess.AccessType.GET;
import static com.tngtech.archunit.core.JavaFieldAccess.AccessType.SET;
import static com.tngtech.archunit.core.JavaFieldAccess.Predicates.accessType;
import static com.tngtech.archunit.core.JavaFieldAccess.getDescriptionTemplateFor;
import static java.util.Collections.singleton;

class FieldAccessCondition extends ArchCondition<JavaFieldAccess> {
    private final DescribedPredicate<? super JavaFieldAccess> fieldAccessIdentifier;
    private final String descriptionTemplate;

    FieldAccessCondition(DescribedPredicate<? super JavaFieldAccess> fieldAccessIdentifier) {
        this(fieldAccessIdentifier, EnumSet.allOf(AccessType.class));
    }

    FieldAccessCondition(DescribedPredicate<? super JavaFieldAccess> fieldAccessIdentifier, Set<AccessType> accessTypes) {
        super(String.format("access field where %s and access type is one of %s",
                fieldAccessIdentifier.getDescription(), accessTypes));

        this.fieldAccessIdentifier = fieldAccessIdentifier;
        this.descriptionTemplate = getDescriptionTemplateFor(accessTypes);
    }

    @Override
    public void check(JavaFieldAccess item, ConditionEvents events) {
        String message = item.getDescriptionWithTemplate(descriptionTemplate);
        events.add(new ConditionEvent(fieldAccessIdentifier.apply(item), message));
    }

    static class FieldGetAccessCondition extends FieldAccessCondition {
        FieldGetAccessCondition(DescribedPredicate<? super JavaFieldAccess> predicate) {
            super(predicate.<JavaFieldAccess>forSubType().and(accessType(GET)), singleton(GET));
        }
    }

    static class FieldSetAccessCondition extends FieldAccessCondition {
        FieldSetAccessCondition(DescribedPredicate<? super JavaFieldAccess> predicate) {
            super(predicate.<JavaFieldAccess>forSubType().and(accessType(SET)), singleton(SET));
        }
    }
}
