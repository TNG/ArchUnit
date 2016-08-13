package com.tngtech.archunit.lang;

import java.util.Collection;
import java.util.Iterator;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

public class ConditionEvents implements Iterable<ConditionEvent> {
    private final Multimap<Type, ConditionEvent> eventsByViolation = ArrayListMultimap.create();

    public void add(ConditionEvent event) {
        eventsByViolation.get(Type.from(event.isViolation())).add(event);
    }

    public Collection<ConditionEvent> getViolating() {
        return eventsByViolation.get(Type.VIOLATION);
    }

    public Collection<ConditionEvent> getAllowed() {
        return eventsByViolation.get(Type.ALLOWED);
    }

    public boolean containViolation() {
        return !getViolating().isEmpty();
    }

    public boolean isEmpty() {
        return getAllowed().isEmpty() && getViolating().isEmpty();
    }

    public void describeFailuresTo(FailureMessages messages) {
        for (ConditionEvent event : getViolating()) {
            event.describeTo(messages);
        }
    }

    @Override
    public Iterator<ConditionEvent> iterator() {
        return ImmutableSet.copyOf(eventsByViolation.values()).iterator();
    }

    @Override
    public String toString() {
        return "ConditionEvents{" +
                "Allowed Events: " + getAllowed() +
                "; Violating Events: " + getViolating() +
                '}';
    }

    enum Type {
        ALLOWED, VIOLATION;

        public static Type from(boolean violation) {
            return violation ? VIOLATION : ALLOWED;
        }
    }
}
