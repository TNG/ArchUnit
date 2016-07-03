package com.tngtech.archunit.testutil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.tngtech.archunit.core.Optional;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.FailureMessages;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.AbstractIterableAssert;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Lists.newArrayList;

public class Assertions extends org.assertj.core.api.Assertions {
    public static ConditionEventsAssert assertThat(ConditionEvents events) {
        return new ConditionEventsAssert(events);
    }

    public static <T> org.assertj.guava.api.OptionalAssert<T> assertThat(Optional<T> optional) {
        return org.assertj.guava.api.Assertions.assertThat(com.google.common.base.Optional.fromNullable(optional.orNull()));
    }

    public static class ConditionEventsAssert extends AbstractIterableAssert<ConditionEventsAssert, ConditionEvents, ConditionEvent> {
        protected ConditionEventsAssert(ConditionEvents actual) {
            super(actual, ConditionEventsAssert.class);
        }

        public void containViolations(String violation, String... additional) {
            assertThat(actual.containViolation()).as("Condition is violated").isTrue();

            List<String> expected = concat(violation, additional);
            if (!sorted(violatingMessages()).equals(sorted(expected))) {
                failWithMessage("Expected %s to contain only violations %s", actual, expected);
            }
        }

        public void containAllowed(String message, String... additional) {
            assertThat(actual.getAllowed().size()).as("Allowed events occurred").isGreaterThan(0);

            List<String> expected = concat(message, additional);
            if (!sorted(messagesOf(actual.getAllowed())).equals(sorted(expected))) {
                failWithMessage("Expected %s to contain only allowed events %s", actual, expected);
            }
        }

        private List<String> violatingMessages() {
            return messagesOf(actual.getViolating());
        }

        private List<String> messagesOf(Collection<ConditionEvent> events) {
            FailureMessages messages = new FailureMessages();
            for (ConditionEvent event : events) {
                event.describeTo(messages);
            }
            return newArrayList(messages);
        }

        private List<String> concat(String violation, String[] additional) {
            ArrayList<String> list = newArrayList(additional);
            list.add(0, violation);
            return list;
        }

        private List<String> sorted(Collection<String> collection) {
            ArrayList<String> result = new ArrayList<>(collection);
            Collections.sort(result);
            return result;
        }

        public void containNoViolation() {
            assertThat(actual.containViolation()).as("Condition is violated").isFalse();
            assertThat(violatingMessages()).as("No violating messages").isEmpty();
        }

        public void haveOneViolationMessageContaining(Set<String> messageParts) {
            assertThat(violatingMessages()).as("Number of violations").hasSize(1);
            AbstractCharSequenceAssert<?, String> assertion = assertThat(getOnlyElement(violatingMessages()));
            for (String part : messageParts) {
                assertion.as("violation message containing " + part).contains(part);
            }
        }
    }
}
