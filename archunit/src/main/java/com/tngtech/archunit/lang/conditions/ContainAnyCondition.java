package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

class ContainAnyCondition<T> extends ArchCondition<Collection<? extends T>> {
    private final ArchCondition<T> condition;

    ContainAnyCondition(ArchCondition<T> condition) {
        super("contain any element that " + condition.getDescription());
        this.condition = condition;
    }

    @Override
    public void check(Collection<? extends T> collection, ConditionEvents events) {
        ConditionEvents subEvents = new ConditionEvents();
        for (T element : collection) {
            condition.check(element, subEvents);
        }
        if (!subEvents.isEmpty()) {
            events.add(new AnyConditionEvent<>(collection, subEvents));
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{condition=" + condition + "}";
    }

    private static class AnyConditionEvent<T> extends SimpleConditionEvent<Collection<T>> {
        private Collection<ConditionEvent> violating;
        private Collection<ConditionEvent> allowed;

        private AnyConditionEvent(Collection<T> correspondingObject, ConditionEvents events) {
            this(correspondingObject, !events.getAllowed().isEmpty(), events.getAllowed(), events.getViolating());
        }

        private AnyConditionEvent(Collection<T> correspondingObject, boolean conditionSatisfied, Collection<ConditionEvent> allowed, Collection<ConditionEvent> violating) {
            super(correspondingObject, conditionSatisfied, joinMessages(violating));
            this.allowed = allowed;
            this.violating = violating;
        }

        @Override
        public void addInvertedTo(ConditionEvents events) {
            events.add(new AnyConditionEvent<>(getCorrespondingObject(), isViolation(), violating, allowed));
        }
    }
}
