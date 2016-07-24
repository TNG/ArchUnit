package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.lang.conditions.FieldAccessCondition.FieldGetAccessCondition;
import com.tngtech.archunit.lang.conditions.FieldAccessCondition.FieldSetAccessCondition;

class ClassAccessesFieldCondition extends AnyAttributeMatchesCondition<JavaFieldAccess> {
    ClassAccessesFieldCondition(DescribedPredicate<JavaFieldAccess> predicate) {
        this(new FieldAccessCondition(predicate));
    }

    ClassAccessesFieldCondition(FieldAccessCondition condition) {
        super(condition.getDescription(), condition);
    }

    @Override
    Collection<JavaFieldAccess> relevantAttributes(JavaClass item) {
        return item.getFieldAccessesFromSelf();
    }

    static class ClassGetsFieldCondition extends ClassAccessesFieldCondition {
        ClassGetsFieldCondition(DescribedPredicate<JavaFieldAccess> predicate) {
            super(new FieldGetAccessCondition(predicate));
        }
    }

    static class ClassSetsFieldCondition extends ClassAccessesFieldCondition {
        ClassSetsFieldCondition(DescribedPredicate<JavaFieldAccess> predicate) {
            super(new FieldSetAccessCondition(predicate));
        }
    }
}
