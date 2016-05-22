package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.google.common.base.Predicate;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.lang.conditions.FieldAccessCondition.FieldGetAccessCondition;
import com.tngtech.archunit.lang.conditions.FieldAccessCondition.FieldSetAccessCondition;

class ClassAccessesFieldCondition extends ClassMatchesAnyCondition<JavaFieldAccess> {
    ClassAccessesFieldCondition(Predicate<JavaFieldAccess> predicate) {
        this(new FieldAccessCondition(predicate));
    }

    ClassAccessesFieldCondition(FieldAccessCondition condition) {
        super(condition);
    }

    @Override
    Collection<JavaFieldAccess> relevantAttributes(JavaClass item) {
        return item.getFieldAccesses();
    }

    static class ClassGetsFieldCondition extends ClassAccessesFieldCondition {
        ClassGetsFieldCondition(Predicate<JavaFieldAccess> predicate) {
            super(new FieldGetAccessCondition(predicate));
        }
    }

    static class ClassSetsFieldCondition extends ClassAccessesFieldCondition {
        ClassSetsFieldCondition(Predicate<JavaFieldAccess> predicate) {
            super(new FieldSetAccessCondition(predicate));
        }
    }
}
