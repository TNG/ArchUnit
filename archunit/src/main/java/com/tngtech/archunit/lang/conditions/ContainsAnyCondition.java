package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;

class ContainsAnyCondition<T> extends ArchCondition<Collection<? extends T>> {
    private final ArchCondition<T> condition;

    ContainsAnyCondition(ArchCondition<T> condition) {
        this.condition = condition;
    }

    @Override
    public void check(Collection<? extends T> collection, ConditionEvents events) {
        ConditionEvents subEvents = new ConditionEvents();
        for (T element : collection) {
            condition.check(element, subEvents);
        }
        events.add(new AnyConditionEvent(subEvents));
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{condition=" + condition + "}";
    }

    private static class AnyConditionEvent extends ConditionEvent {
        private Collection<ConditionEvent> violating;
        private Collection<ConditionEvent> allowed;

        private AnyConditionEvent(ConditionEvents events) {
            this(!events.getAllowed().isEmpty(), events.getAllowed(), events.getViolating());
        }

        private AnyConditionEvent(boolean conditionSatisfied, Collection<ConditionEvent> allowed, Collection<ConditionEvent> violating) {
            super(conditionSatisfied, joinMessages(violating));
            this.allowed = allowed;
            this.violating = violating;
        }

        @Override
        public void addInvertedTo(ConditionEvents events) {
            events.add(new AnyConditionEvent(isViolation(), violating, allowed));
        }
    }
}
