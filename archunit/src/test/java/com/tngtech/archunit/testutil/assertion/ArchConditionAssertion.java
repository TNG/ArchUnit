package com.tngtech.archunit.testutil.assertion;

import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import org.assertj.core.api.AbstractObjectAssert;

import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class ArchConditionAssertion<T> extends AbstractObjectAssert<ArchConditionAssertion<T>, ArchCondition<T>> {

    public ArchConditionAssertion(ArchCondition<T> actual) {
        super(actual, ArchConditionAssertion.class);
    }

    public ArchConditionAssertion<T> hasDescription(String description) {
        assertThat(actual.getDescription()).as("description").isEqualTo(description);
        return this;
    }

    public ConditionEventsAssertion checking(T item) {
        ConditionEvents events = new ConditionEvents();
        actual.check(item, events);
        return assertThat(events);
    }
}
