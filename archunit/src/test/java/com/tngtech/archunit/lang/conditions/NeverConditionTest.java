package com.tngtech.archunit.lang.conditions;

import java.util.Collections;

import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchConditionTest.ConditionWithInit;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.Test;

import static com.tngtech.archunit.lang.ArchConditionTest.someCondition;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class NeverConditionTest {
    private static final String ORIGINALLY_MISMATCH = "originally mismatch";
    private static final String ORIGINALLY_NO_MISMATCH = "originally no mismatch";

    private static final ArchCondition<Object> ONE_VIOLATED_ONE_SATISFIED = new ArchCondition<Object>("irrelevant") {
        @Override
        public void check(Object item, ConditionEvents events) {
            events.add(new SimpleConditionEvent<>(item, false, ORIGINALLY_MISMATCH));
            events.add(new SimpleConditionEvent<>(item, true, ORIGINALLY_NO_MISMATCH));
        }
    };

    @Test
    public void inverts_condition() {
        ConditionEvents events = new ConditionEvents();
        ONE_VIOLATED_ONE_SATISFIED.check(new Object(), events);

        assertThat(events).containAllowed(ORIGINALLY_NO_MISMATCH);
        assertThat(events).containViolations(ORIGINALLY_MISMATCH);

        events = new ConditionEvents();
        never(ONE_VIOLATED_ONE_SATISFIED).check(new Object(), events);

        assertThat(events).containAllowed(ORIGINALLY_MISMATCH);
        assertThat(events).containViolations(ORIGINALLY_NO_MISMATCH);
    }

    @Test
    public void updates_description() {
        assertThat(never(someCondition("anything")).getDescription()).isEqualTo("never anything");
    }

    @Test
    public void inits_inverted_condition() {
        ConditionWithInit original = someCondition("anything");
        ArchCondition<String> never = never(original);
        never.init(Collections.singleton("something"));

        assertThat(original.allObjectsToTest).containsExactly("something");
    }
}