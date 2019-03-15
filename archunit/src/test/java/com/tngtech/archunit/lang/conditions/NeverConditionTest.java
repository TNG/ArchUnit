package com.tngtech.archunit.lang.conditions;

import java.util.Collections;

import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchConditionTest.ConditionWithInitAndFinish;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.ArchConditionTest.someCondition;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;

@RunWith(DataProviderRunner.class)
public class NeverConditionTest {
    private static final String ORIGINALLY_MISMATCH = "originally mismatch";
    private static final String ORIGINALLY_NO_MISMATCH = "originally no mismatch";

    private static final ArchCondition<Object> ONE_VIOLATED_ONE_SATISFIED =
            new ArchCondition<Object>("one violated, one satisfied in check") {
                @Override
                public void check(Object item, ConditionEvents events) {
                    addOneViolatedOneSatisfied(item, events);
                }
            };

    private static final ArchCondition<Object> ONE_VIOLATED_ONE_SATISFIED_IN_FINISH =
            new ArchCondition<Object>("one violated, one satisfied in finish") {
                @Override
                public void check(Object item, ConditionEvents events) {
                }

                @Override
                public void finish(ConditionEvents events) {
                    addOneViolatedOneSatisfied(new Object(), events);
                }
            };

    private static void addOneViolatedOneSatisfied(Object item, ConditionEvents events) {
        events.add(new SimpleConditionEvent(item, false, ORIGINALLY_MISMATCH));
        events.add(new SimpleConditionEvent(item, true, ORIGINALLY_NO_MISMATCH));
    }

    @DataProvider
    public static Object[][] conditions() {
        return testForEach(ONE_VIOLATED_ONE_SATISFIED, ONE_VIOLATED_ONE_SATISFIED_IN_FINISH);
    }

    @Test
    @UseDataProvider("conditions")
    public void inverts_condition(ArchCondition<Object> condition) {
        ConditionEvents events = new ConditionEvents();
        condition.check(new Object(), events);
        condition.finish(events);

        assertThat(events).containAllowed(ORIGINALLY_NO_MISMATCH);
        assertThat(events).containViolations(ORIGINALLY_MISMATCH);

        events = new ConditionEvents();
        never(condition).check(new Object(), events);
        never(condition).finish(events);

        assertThat(events).containAllowed(ORIGINALLY_MISMATCH);
        assertThat(events).containViolations(ORIGINALLY_NO_MISMATCH);
    }

    @Test
    public void updates_description() {
        assertThat(never(someCondition("anything"))).hasDescription("never anything");
    }

    @Test
    public void inits_inverted_condition() {
        ConditionWithInitAndFinish original = someCondition("anything");
        ArchCondition<String> never = never(original);
        never.init(Collections.singleton("something"));

        assertThat(original.allObjectsToTest).containsExactly("something");
    }
}