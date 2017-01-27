package com.tngtech.archunit.lang;

import java.util.Arrays;

import org.junit.Test;

import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
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
        ArchCondition<Integer> greaterThanTenFourteenAndTwenty = greaterThan(10).and(greaterThan(14, 20));

        ConditionEvents events = new ConditionEvents();
        greaterThanTenFourteenAndTwenty.check(15, events);
        assertThat(events).containViolations("15 is not greater than 20");

        events = new ConditionEvents();
        greaterThanTenFourteenAndTwenty.check(5, events);
        assertThat(events).containViolations(
                "5 is not greater than 10",
                "5 is not greater than 14",
                "5 is not greater than 20");

        events = new ConditionEvents();
        greaterThanTenFourteenAndTwenty.check(21, events);
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

    @Test
    public void never_and() {
        ArchCondition<Integer> condition = never(greaterThan(3, 9).and(greaterThan(5, 7)));

        ConditionEvents events = new ConditionEvents();
        condition.check(4, events);
        assertThat(events.containViolation()).as("Events contain violation").isFalse();

        events = new ConditionEvents();
        condition.check(6, events);
        assertThat(events.containViolation()).as("Events contain violation").isTrue();
    }

    @Test
    public void double_never_and() {
        ArchCondition<Integer> condition = never(never(greaterThan(3, 9).and(greaterThan(5, 7))));

        ConditionEvents events = new ConditionEvents();
        condition.check(9, events);
        assertThat(events.containViolation()).as("Events contain violation").isTrue();

        events = new ConditionEvents();
        condition.check(10, events);
        assertThat(events.containViolation()).as("Events contain violation").isFalse();
    }

    private ArchCondition<Integer> greaterThan(final int... numbers) {
        return new ArchCondition<Integer>("greater than " + Arrays.toString(numbers)) {
            @Override
            public void check(final Integer item, ConditionEvents events) {
                for (int number : numbers) {
                    events.add(new GreaterThanEvent(item, number));
                }
            }
        };
    }

    public static ConditionWithInit someCondition(String description) {
        return new ConditionWithInit(description);
    }

    private static class GreaterThanEvent extends SimpleConditionEvent {
        GreaterThanEvent(int item, int number) {
            super(item > number, String.format(
                    "%d is%s greater than %d",
                    item, item <= number ? " not" : "", number));
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