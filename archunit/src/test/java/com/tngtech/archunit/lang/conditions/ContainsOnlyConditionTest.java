package com.tngtech.archunit.lang.conditions;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.Test;

import static com.tngtech.archunit.lang.conditions.ArchConditions.containOnlyElementsThat;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public class ContainsOnlyConditionTest {
    private static final List<SerializableObject> TWO_SERIALIZABLE_OBJECTS = asList(new SerializableObject(), new SerializableObject());
    static final List<Object> ONE_SERIALIZABLE_AND_ONE_NON_SERIALIZABLE_OBJECT = asList(new SerializableObject(), new Object());

    static final ArchCondition<Object> IS_SERIALIZABLE = new ArchCondition<Object>("be serializable") {
        @Override
        public void check(Object item, ConditionEvents events) {
            boolean satisfied = item instanceof Serializable;
            events.add(new SimpleConditionEvent(item, satisfied, isSerializableMessageFor(item.getClass())));
        }
    };

    static String isSerializableMessageFor(Class<?> clazz) {
        return String.format("%s is%s serializable", clazz.getSimpleName(), Serializable.class.isAssignableFrom(clazz) ? "" : " not");
    }

    @Test
    public void satisfied_works_and_description_contains_mismatches() {
        ConditionEvents events = ConditionEvents.Factory.create();
        containOnlyElementsThat(IS_SERIALIZABLE).check(ONE_SERIALIZABLE_AND_ONE_NON_SERIALIZABLE_OBJECT, events);

        assertThat(events).containViolations(isSerializableMessageFor(Object.class));

        events = ConditionEvents.Factory.create();
        containOnlyElementsThat(IS_SERIALIZABLE).check(TWO_SERIALIZABLE_OBJECTS, events);

        assertThat(events).containNoViolation();
    }

    @Test
    public void inverting_works() {
        ConditionEvents events = ConditionEvents.Factory.create();
        ArchCondition<Collection<?>> condition = containOnlyElementsThat(IS_SERIALIZABLE);

        condition.check(TWO_SERIALIZABLE_OBJECTS, events);
        assertThat(events).containNoViolation();

        events = ConditionEvents.Factory.create();
        never(condition).check(TWO_SERIALIZABLE_OBJECTS, events);
        assertThat(events).containViolations(messageForTwoTimes(isSerializableMessageFor(SerializableObject.class)));
    }

    @Test
    public void if_there_are_no_input_events_no_ContainsOnlyEvent_is_added() {
        ViolatedAndSatisfiedConditionEvents events = new ViolatedAndSatisfiedConditionEvents();
        containOnlyElementsThat(IS_SERIALIZABLE).check(emptyList(), events);
        assertThat(events.getAllowed()).as("allowed events").isEmpty();
        assertThat(events.getViolating()).as("violated events").isEmpty();
    }

    static String messageForTwoTimes(String message) {
        return String.format("%s%n%s", message, message);
    }

    static class SerializableObject implements Serializable {
    }
}
