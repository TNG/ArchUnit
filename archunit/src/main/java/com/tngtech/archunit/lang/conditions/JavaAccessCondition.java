package com.tngtech.archunit.lang.conditions;

import com.tngtech.archunit.core.DescribedPredicate;
import com.tngtech.archunit.core.JavaAccess;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;

class JavaAccessCondition extends ArchCondition<JavaAccess<?>> {
    private final DescribedPredicate<JavaAccess<?>> predicate;

    JavaAccessCondition(DescribedPredicate<JavaAccess<?>> predicate) {
        super(predicate.getDescription());
        this.predicate = predicate;
    }

    @Override
    public void check(JavaAccess<?> item, ConditionEvents events) {
        events.add(new ConditionEvent(predicate.apply(item), item.getDescription()));
    }
}
