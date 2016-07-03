package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.tngtech.archunit.core.FluentPredicate;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.lang.conditions.FieldAccessCondition.FieldGetAccessCondition;
import com.tngtech.archunit.lang.conditions.FieldAccessCondition.FieldSetAccessCondition;

class ClassAccessesFieldCondition extends ClassMatchesAnyCondition<JavaFieldAccess> {
    ClassAccessesFieldCondition(FluentPredicate<JavaFieldAccess> predicate) {
        this(new FieldAccessCondition(predicate));
    }

    ClassAccessesFieldCondition(FieldAccessCondition condition) {
        super(condition);
    }

    @Override
    Collection<JavaFieldAccess> relevantAttributes(JavaClass item) {
        return item.getFieldAccessesFromSelf();
    }

    static class ClassGetsFieldCondition extends ClassAccessesFieldCondition {
        ClassGetsFieldCondition(FluentPredicate<JavaFieldAccess> predicate) {
            super(new FieldGetAccessCondition(predicate));
        }
    }

    static class ClassSetsFieldCondition extends ClassAccessesFieldCondition {
        ClassSetsFieldCondition(FluentPredicate<JavaFieldAccess> predicate) {
            super(new FieldSetAccessCondition(predicate));
        }
    }
}
