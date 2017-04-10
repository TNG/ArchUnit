package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.lang.conditions.FieldAccessCondition.FieldGetAccessCondition;
import com.tngtech.archunit.lang.conditions.FieldAccessCondition.FieldSetAccessCondition;

class ClassAccessesFieldCondition extends AnyAttributeMatchesCondition<JavaFieldAccess> {
    ClassAccessesFieldCondition(DescribedPredicate<? super JavaFieldAccess> predicate) {
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
        ClassGetsFieldCondition(DescribedPredicate<? super JavaFieldAccess> predicate) {
            super(new FieldGetAccessCondition(predicate));
        }
    }

    static class ClassSetsFieldCondition extends ClassAccessesFieldCondition {
        ClassSetsFieldCondition(DescribedPredicate<? super JavaFieldAccess> predicate) {
            super(new FieldSetAccessCondition(predicate));
        }
    }
}
