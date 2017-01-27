package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

class ContainsOnlyCondition<T> extends ArchCondition<Collection<? extends T>> {
    private final ArchCondition<T> condition;

    ContainsOnlyCondition(ArchCondition<T> condition) {
        super("contain only elements that " + condition.getDescription());
        this.condition = condition;
    }

    @Override
    public void check(Collection<? extends T> collection, ConditionEvents events) {
        ConditionEvents subEvents = new ConditionEvents();
        for (T item : collection) {
            condition.check(item, subEvents);
        }
        if (!subEvents.isEmpty()) {
            events.add(new OnlyConditionEvent(subEvents));
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{condition=" + condition + "}";
    }

    private static class OnlyConditionEvent extends SimpleConditionEvent {
        private Collection<ConditionEvent> allowed;
        private Collection<ConditionEvent> violating;

        public OnlyConditionEvent(ConditionEvents events) {
            this(!events.containViolation(), events.getAllowed(), events.getViolating());
        }

        public OnlyConditionEvent(boolean conditionSatisfied, Collection<ConditionEvent> allowed, Collection<ConditionEvent> violating) {
            super(conditionSatisfied, joinMessages(violating));
            this.allowed = allowed;
            this.violating = violating;
        }

        @Override
        public void addInvertedTo(ConditionEvents events) {
            events.add(new OnlyConditionEvent(isViolation(), violating, allowed));
        }
    }
}
