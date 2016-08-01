package com.tngtech.archunit.lang;

import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class ArchConditionTest {

    @Test
    public void and_works() {
        ArchCondition<Integer> greaterThanTenAndTwenty = greaterThan(10).and(greaterThan(20));

        ConditionEvents events = new ConditionEvents();
        greaterThanTenAndTwenty.check(15, events);
        assertThat(events).containViolations("15 is not greater than 20");

        events = new ConditionEvents();
        greaterThanTenAndTwenty.check(21, events);
        assertThat(events).containNoViolation();
    }

    private ArchCondition<Integer> greaterThan(final int number) {
        return new ArchCondition<Integer>("greater than " + number) {
            @Override
            public void check(final Integer item, ConditionEvents events) {
                events.add(new GreaterThanEvent(item, number));
            }
        };
    }

    private static class GreaterThanEvent extends ConditionEvent {
        public GreaterThanEvent(Integer item, int number) {
            super(item > number, String.format("%d is not greater than %d", item, number));
        }
    }
}