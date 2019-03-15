package com.tngtech.archunit.lang;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static java.util.Collections.singleton;

@RunWith(DataProviderRunner.class)
public class ArchConditionTest {
    @Test
    public void as_inits_delegate() {
        ConditionWithInitAndFinish original = someCondition("any");

        original.as("changed").init(singleton("init"));

        assertThat(original.allObjectsToTest).containsExactly("init");
    }

    @Test
    public void as_finishes_delegate() {
        ConditionWithInitAndFinish original = someCondition("any");

        ConditionEvents events = new ConditionEvents();
        original.as("changed").finish(events);

        assertThat(original.eventsFromFinish).isEqualTo(events);
    }

    @Test
    public void and_checks_all_conditions() {
        ArchCondition<Integer> greaterThan10_14And20 = greaterThan(10).and(greaterThan(14, 20));

        ConditionEvents events = new ConditionEvents();
        greaterThan10_14And20.check(15, events);
        assertThat(events).containViolations("15 is not greater than 20");

        events = new ConditionEvents();
        greaterThan10_14And20.check(5, events);
        assertThat(events).containViolations(
                "5 is not greater than 10",
                "5 is not greater than 14",
                "5 is not greater than 20");

        events = new ConditionEvents();
        greaterThan10_14And20.check(21, events);
        assertThat(events).containNoViolation();
    }

    @Test
    public void and_handles_each_violating_object_separately() {
        ArchCondition<Integer> condition = greaterThan(1).and(greaterThan(2)).and(greaterThan(3));

        ConditionEvents events = new ConditionEvents();
        condition.check(2, events);
        final List<HandledViolation> handledViolations = new ArrayList<>();
        events.handleViolations(new ViolationHandler<Integer>() {
            @Override
            public void handle(Collection<Integer> violatingObjects, String message) {
                handledViolations.add(new HandledViolation(violatingObjects, message));
            }
        });

        assertThat(handledViolations).containsOnly(
                new HandledViolation(2, "2 is not greater than 2"),
                new HandledViolation(2, "2 is not greater than 3"));
    }

    @Test
    public void or_checks_all_conditions() {
        ArchCondition<Integer> greaterThan15OrGreater14And20 =
                greaterThan(15).or(greaterThan(14, 20));

        ConditionEvents events = new ConditionEvents();
        greaterThan15OrGreater14And20.check(15, events);
        assertThat(events).containViolations("15 is not greater than 15 and 15 is not greater than 20");

        events = new ConditionEvents();
        greaterThan15OrGreater14And20.check(5, events);
        assertThat(events).containViolations(
                "5 is not greater than 14 and 5 is not greater than 15 and 5 is not greater than 20");

        events = new ConditionEvents();
        greaterThan15OrGreater14And20.check(16, events);
        assertThat(events).containNoViolation();
    }

    @Test
    public void or_events_join_descriptions() {
        ArchCondition<Integer> isGreaterThan15OrEndsWith1 =
                greaterThan(15).or(endsWith(1));

        ConditionEvents events = new ConditionEvents();
        isGreaterThan15OrEndsWith1.check(12, events);
        assertThat(events).containViolations("12 does not end with 1 and 12 is not greater than 15");
    }

    @Test
    public void or_handles_all_violated_conditions_as_unit() {
        ArchCondition<Integer> condition = greaterThan(1).or(greaterThan(2)).or(greaterThan(3));

        ConditionEvents events = new ConditionEvents();
        condition.check(1, events);
        final List<HandledViolation> handledViolations = new ArrayList<>();
        events.handleViolations(new ViolationHandler<Integer>() {
            @Override
            public void handle(Collection<Integer> violatingObjects, String message) {
                handledViolations.add(new HandledViolation(violatingObjects, message));
            }
        });

        assertThat(handledViolations).containsOnly(new HandledViolation(
                1, "1 is not greater than 1 and 1 is not greater than 2 and 1 is not greater than 3"));
    }

    @DataProvider
    public static Object[][] conditionCombinations() {
        return $$(
                $(new ConditionCombination("and") {
                    @Override
                    <T> ArchCondition<T> combine(ArchCondition<T> first, ArchCondition<T> second) {
                        return first.and(second);
                    }
                }),
                $(new ConditionCombination("or") {
                    @Override
                    <T> ArchCondition<T> combine(ArchCondition<T> first, ArchCondition<T> second) {
                        return first.or(second);
                    }
                })
        );
    }

    @Test
    @UseDataProvider("conditionCombinations")
    public void join_inits_all_conditions(ConditionCombination combination) {
        ConditionWithInitAndFinish one = someCondition("one");
        ConditionWithInitAndFinish two = someCondition("two");

        combination.combine(one, two).init(singleton("init"));

        assertThat(one.allObjectsToTest).containsExactly("init");
        assertThat(two.allObjectsToTest).containsExactly("init");
    }

    @Test
    @UseDataProvider("conditionCombinations")
    public void join_finishes_all_conditions(ConditionCombination combination) {
        ConditionWithInitAndFinish one = someCondition("one");
        ConditionWithInitAndFinish two = someCondition("two");

        ConditionEvents events = new ConditionEvents();
        combination.combine(one, two).finish(events);

        assertThat(one.eventsFromFinish).isEqualTo(events);
        assertThat(two.eventsFromFinish).isEqualTo(events);
    }

    @Test
    @UseDataProvider("conditionCombinations")
    public void and_joins_descriptions(ConditionCombination combination) {
        ConditionWithInitAndFinish one = someCondition("one");
        ConditionWithInitAndFinish two = someCondition("two");

        ArchCondition<String> joined = combination.combine(one, two);

        assertThat(joined).hasDescription("one " + combination.joinWord + " two");
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

    @Test
    public void never_or() {
        ArchCondition<Integer> condition = never(greaterThan(3, 9).or(greaterThan(5, 7)));

        ConditionEvents events = new ConditionEvents();
        condition.check(3, events);
        assertThat(events.containViolation()).as("Events contain violation").isFalse();

        events = new ConditionEvents();
        condition.check(4, events);
        assertThat(events.containViolation()).as("Events contain violation").isTrue();
    }

    @Test
    public void double_never_or() {
        ArchCondition<Integer> condition = never(never(greaterThan(3, 9).or(greaterThan(5, 7))));

        ConditionEvents events = new ConditionEvents();
        condition.check(7, events);
        assertThat(events.containViolation()).as("Events contain violation").isTrue();

        events = new ConditionEvents();
        condition.check(8, events);
        assertThat(events.containViolation()).as("Events contain violation").isFalse();
    }

    private ArchCondition<Integer> greaterThan(final int... numbers) {
        return new ArchCondition<Integer>("greater than " + Arrays.toString(numbers)) {
            @Override
            public void check(final Integer item, ConditionEvents events) {
                for (int number : numbers) {
                    events.add(greaterThanEvent(item, number));
                }
            }
        };
    }

    private ArchCondition<Integer> endsWith(final int number) {
        return new ArchCondition<Integer>("ends with " + number) {
            @Override
            public void check(final Integer item, ConditionEvents events) {
                boolean matches = item.toString().endsWith(Integer.toString(number));
                events.add(new SimpleConditionEvent(item, matches,
                        item + (matches ? " ends with " : " does not end with ") + number));
            }
        };
    }

    public static ConditionWithInitAndFinish someCondition(String description) {
        return new ConditionWithInitAndFinish(description);
    }

    private ConditionEvent greaterThanEvent(Integer item, int number) {
        return new SimpleConditionEvent(item, item > number,
                String.format("%d is%s greater than %d",
                        item, item <= number ? " not" : "", number));
    }

    public static class ConditionWithInitAndFinish extends ArchCondition<String> {
        public Iterable<String> allObjectsToTest;
        ConditionEvents eventsFromFinish;

        ConditionWithInitAndFinish(String description) {
            super(description);
        }

        @Override
        public void init(Iterable<String> allObjectsToTest) {
            this.allObjectsToTest = allObjectsToTest;
        }

        @Override
        public void check(String item, ConditionEvents events) {
        }

        @Override
        public void finish(ConditionEvents events) {
            this.eventsFromFinish = events;
        }
    }

    private abstract static class ConditionCombination {
        private final String joinWord;

        ConditionCombination(String joinWord) {
            this.joinWord = joinWord;
        }

        abstract <T> ArchCondition<T> combine(ArchCondition<T> first, ArchCondition<T> second);
    }

    private static class HandledViolation {
        final Multiset<Integer> objects;
        final String message;

        HandledViolation(int object, String message) {
            this(singleton(object), message);
        }

        HandledViolation(Collection<Integer> objects, String message) {
            this.objects = HashMultiset.create(objects);
            this.message = message;
        }

        @Override
        public int hashCode() {
            return Objects.hash(objects, message);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final HandledViolation other = (HandledViolation) obj;
            return Objects.equals(this.objects, other.objects)
                    && Objects.equals(this.message, other.message);
        }

        @Override
        public String toString() {
            return "HandledViolation{" +
                    "objects=" + objects +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}