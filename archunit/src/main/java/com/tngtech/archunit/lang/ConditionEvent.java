package com.tngtech.archunit.lang;

import java.util.Collection;

import com.google.common.base.Function;
import com.google.common.base.Joiner;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.transform;

public class ConditionEvent {
    private final boolean conditionSatisfied;
    private final String message;

    public ConditionEvent(boolean conditionSatisfied, String messageTemplate, Object... args) {
        this.conditionSatisfied = conditionSatisfied;
        this.message = String.format(messageTemplate, args);
        checkArgument(conditionSatisfied || !message.trim().isEmpty(), "Message may not be empty for violation");
    }

    public boolean isViolation() {
        return !conditionSatisfied;
    }

    public void addInvertedTo(ConditionEvents events) {
        events.add(new ConditionEvent(!conditionSatisfied, message));
    }

    public void describeTo(CollectsLines messages) {
        messages.add(message);
    }

    @Override
    public String toString() {
        return "ConditionEvent{conditionSatisfied=" + conditionSatisfied + ", message='" + message + "'}";
    }

    public static String joinMessages(Collection<ConditionEvent> violating) {
        return Joiner.on(System.lineSeparator()).join(transform(violating, TO_MESSAGE));
    }

    private static final Function<ConditionEvent, String> TO_MESSAGE = new Function<ConditionEvent, String>() {
        @Override
        public String apply(ConditionEvent input) {
            return input.message;
        }
    };

    public static ConditionEvent violated(String message, Object... args) {
        return new ConditionEvent(false, message, args);
    }

    public static ConditionEvent satisfied(String message, Object... args) {
        return new ConditionEvent(true, message, args);
    }
}
