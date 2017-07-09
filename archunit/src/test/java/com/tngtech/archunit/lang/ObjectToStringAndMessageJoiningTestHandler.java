package com.tngtech.archunit.lang;

import java.util.Collection;
import java.util.Set;

import com.google.common.base.Joiner;
import com.tngtech.archunit.lang.ConditionEventsTest.CorrectType;

class ObjectToStringAndMessageJoiningTestHandler implements ViolationHandler<CorrectType> {
    private final Set<String> messages;

    ObjectToStringAndMessageJoiningTestHandler(Set<String> messages) {
        this.messages = messages;
    }

    @Override
    public void handle(Collection<CorrectType> violatingObjects, String message) {
        messages.add(Joiner.on(", ").join(violatingObjects) + ": " + message);
    }
}
