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
            events.add(new OnlyConditionEvent<>(collection, subEvents));
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{condition=" + condition + "}";
    }

    private static class OnlyConditionEvent<T> extends SimpleConditionEvent<Collection<T>> {
        private Collection<ConditionEvent> allowed;
        private Collection<ConditionEvent> violating;

        private OnlyConditionEvent(Collection<T> correspondingObject, ConditionEvents events) {
            this(correspondingObject, !events.containViolation(), events.getAllowed(), events.getViolating());
        }

        private OnlyConditionEvent(Collection<T> correspondingObject,
                                   boolean conditionSatisfied,
                                   Collection<ConditionEvent> allowed,
                                   Collection<ConditionEvent> violating) {
            super(correspondingObject, conditionSatisfied, joinMessages(violating));
            this.allowed = allowed;
            this.violating = violating;
        }

        @Override
        public void addInvertedTo(ConditionEvents events) {
            events.add(new OnlyConditionEvent<>(getCorrespondingObject(), isViolation(), violating, allowed));
        }
    }
}
