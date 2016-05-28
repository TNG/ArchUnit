package com.tngtech.archunit.lang.conditions;

import com.tngtech.archunit.lang.AbstractArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;

class NeverCondition<T> extends AbstractArchCondition<T> {
    private final AbstractArchCondition<T> condition;

    NeverCondition(AbstractArchCondition<T> condition) {
        this.condition = condition;
    }

    @Override
    public void check(T item, ConditionEvents events) {
        ConditionEvents subEvents = new ConditionEvents();
        condition.check(item, subEvents);
        for (ConditionEvent event : subEvents) {
            event.addInvertedTo(events);
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{condition=" + condition + "}";
    }

}
