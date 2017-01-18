package com.tngtech.archunit.lang;

import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static java.util.Collections.singleton;

public class ArchConditionTest {
    @Test
    public void as_inits_delegate() {
        ConditionWithInit original = someCondition("any");

        original.as("changed").init(singleton("init"));

        assertThat(original.allObjectsToTest).containsExactly("init");
    }

    @Test
    public void and_checks_all_conditions() {
        ArchCondition<Integer> greaterThanTenAndTwenty = greaterThan(10).and(greaterThan(20));

        ConditionEvents events = new ConditionEvents();
        greaterThanTenAndTwenty.check(15, events);
        assertThat(events).containViolations("15 is not greater than 20");

        events = new ConditionEvents();
        greaterThanTenAndTwenty.check(21, events);
        assertThat(events).containNoViolation();
    }

    @Test
    public void and_inits_all_conditions() {
        ConditionWithInit one = someCondition("one");
        ConditionWithInit two = someCondition("two");

        one.and(two).init(singleton("init"));

        assertThat(one.allObjectsToTest).containsExactly("init");
        assertThat(two.allObjectsToTest).containsExactly("init");
    }

    @Test
    public void and_joins_descriptions() {
        ConditionWithInit one = someCondition("one");
        ConditionWithInit two = someCondition("two");

        assertThat(one.and(two).getDescription()).isEqualTo("one and two");
    }

    private ArchCondition<Integer> greaterThan(final int number) {
        return new ArchCondition<Integer>("greater than " + number) {
            @Override
            public void check(final Integer item, ConditionEvents events) {
                events.add(new GreaterThanEvent(item, number));
            }
        };
    }

    public static ConditionWithInit someCondition(String description) {
        return new ConditionWithInit(description);
    }

    private static class GreaterThanEvent extends ConditionEvent {
        public GreaterThanEvent(Integer item, int number) {
            super(item > number, String.format("%d is not greater than %d", item, number));
        }
    }

    public static class ConditionWithInit extends ArchCondition<String> {
        public Iterable<String> allObjectsToTest;

        ConditionWithInit(String description) {
            super(description);
        }

        @Override
        public void init(Iterable<String> allObjectsToTest) {
            this.allObjectsToTest = allObjectsToTest;
        }

        @Override
        public void check(String item, ConditionEvents events) {
        }
    }
}