package com.tngtech.archunit.lang.conditions;

import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;

class NeverCondition<T> extends ArchCondition<T> {
    private final ArchCondition<T> condition;

    NeverCondition(ArchCondition<T> condition) {
        super("never " + condition.getDescription());
        this.condition = condition;
    }

    @Override
    public void init(Iterable<T> allObjectsToTest) {
        condition.init(allObjectsToTest);
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
