package com.tngtech.archunit.lang;

public interface ConditionEvent {
    boolean isViolation();

    void addInvertedTo(ConditionEvents events);

    void describeTo(CollectsLines messages);
}
