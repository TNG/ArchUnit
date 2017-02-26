package com.tngtech.archunit.lang;

public interface ConditionEvent<T> {
    boolean isViolation();

    void addInvertedTo(ConditionEvents events);

    void describeTo(CollectsLines messages);

    T getCorrespondingObject();
}
