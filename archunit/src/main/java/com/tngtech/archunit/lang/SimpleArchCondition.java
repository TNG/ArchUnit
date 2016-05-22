package com.tngtech.archunit.lang;

import com.google.common.base.Predicate;

public class SimpleArchCondition<T> extends ArchCondition<T> {
    private final Predicate<T> predicate;
    private final Message<T> message;

    public static <T> Creator<T> violationIf(Predicate<T> violationDetector) {
        return new Creator<>(violationDetector);
    }

    private SimpleArchCondition(Predicate<T> predicate, Message<T> message) {
        this.predicate = predicate;
        this.message = message;
    }

    @Override
    public final void check(T item, ConditionEvents events) {
        events.add(new ConditionEvent(!predicate.apply(item), message.createFor(item)));
    }

    public interface Message<T> {
        String createFor(T item);
    }

    public static class Creator<T> {
        private final Predicate<T> violationDetector;

        private Creator(Predicate<T> violationDetector) {
            this.violationDetector = violationDetector;
        }

        public SimpleArchCondition<T> withMessage(Message<T> message) {
            return new SimpleArchCondition<>(violationDetector, message);
        }
    }
}
