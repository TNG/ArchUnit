package com.tngtech.archunit.lang;

import java.util.Set;

import com.tngtech.archunit.lang.ConditionEventsTest.CorrectType;

class ObjectToStringAndMessageJoiningTestHandler implements ViolationHandler<CorrectType> {
    private final Set<String> messages;

    ObjectToStringAndMessageJoiningTestHandler(Set<String> messages) {
        this.messages = messages;
    }

    @Override
    public void handle(CorrectType violatingObject, String message) {
        messages.add(violatingObject + ": " + message);
    }
}
