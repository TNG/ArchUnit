package com.tngtech.archunit.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Joiner;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.transform;

public class SimpleConditionEvent implements ConditionEvent {
    private final boolean conditionSatisfied;
    private final String message;

    public SimpleConditionEvent(boolean conditionSatisfied, String messageTemplate, Object... args) {
        this.conditionSatisfied = conditionSatisfied;
        this.message = String.format(messageTemplate, args);
        checkArgument(conditionSatisfied || !message.trim().isEmpty(), "Message may not be empty for violation");
    }

    @Override
    public boolean isViolation() {
        return !conditionSatisfied;
    }

    @Override
    public void addInvertedTo(ConditionEvents events) {
        events.add(new SimpleConditionEvent(!conditionSatisfied, message));
    }

    @Override
    public void describeTo(CollectsLines messages) {
        messages.add(message);
    }

    @Override
    public String toString() {
        return "ConditionEvent{conditionSatisfied=" + conditionSatisfied + ", message='" + message + "'}";
    }

    protected static String joinMessages(Collection<ConditionEvent> violating) {
        Iterable<String> lines = concat(transform(violating, TO_MESSAGES));
        return Joiner.on(System.lineSeparator()).join(lines);
    }

    private static final Function<ConditionEvent, Iterable<String>> TO_MESSAGES = new Function<ConditionEvent, Iterable<String>>() {
        @Override
        public Iterable<String> apply(ConditionEvent input) {
            final List<String> result = new ArrayList<>();
            input.describeTo(new CollectsLines() {
                @Override
                public void add(String line) {
                    result.add(line);
                }
            });
            return result;
        }
    };

    public static ConditionEvent violated(String message, Object... args) {
        return new SimpleConditionEvent(false, message, args);
    }

    public static ConditionEvent satisfied(String message, Object... args) {
        return new SimpleConditionEvent(true, message, args);
    }
}
