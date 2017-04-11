package com.tngtech.archunit.lang;

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

@PublicAPI(usage = INHERITANCE)
public interface ConditionEvent<T> {
    boolean isViolation();

    void addInvertedTo(ConditionEvents events);

    void describeTo(CollectsLines messages);

    T getCorrespondingObject();
}
