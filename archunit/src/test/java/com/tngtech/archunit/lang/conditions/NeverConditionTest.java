package com.tngtech.archunit.lang.conditions;

import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import org.junit.Test;

import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class NeverConditionTest {
    public static final String ORIGINALLY_MISMATCH = "originally mismatch";
    public static final String ORIGINALLY_NO_MISMATCH = "originally no mismatch";

    public static final ConditionEvent ORIGINALLY_VIOLATING_EVENT = new ConditionEvent(false, ORIGINALLY_MISMATCH);

    public static final ConditionEvent ORIGINALLY_ALLOWED_EVENT = new ConditionEvent(true, ORIGINALLY_NO_MISMATCH);

    public static final ArchCondition<Object> ONE_VIOLATED_ONE_SATISFIED = new ArchCondition<Object>() {
        @Override
        public void check(Object item, ConditionEvents events) {
            events.add(ORIGINALLY_VIOLATING_EVENT);
            events.add(ORIGINALLY_ALLOWED_EVENT);
        }
    };

    @Test
    public void satisfied_is_correct_and_description_is_inverted() {
        ConditionEvents events = new ConditionEvents();
        ONE_VIOLATED_ONE_SATISFIED.check(new Object(), events);

        assertThat(events).containAllowed(ORIGINALLY_NO_MISMATCH);
        assertThat(events).containViolations(ORIGINALLY_MISMATCH);

        events = new ConditionEvents();
        never(ONE_VIOLATED_ONE_SATISFIED).check(new Object(), events);

        assertThat(events).containAllowed(ORIGINALLY_MISMATCH);
        assertThat(events).containViolations(ORIGINALLY_NO_MISMATCH);
    }
}