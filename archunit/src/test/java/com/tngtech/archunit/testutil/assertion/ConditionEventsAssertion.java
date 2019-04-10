package com.tngtech.archunit.testutil.assertion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.AbstractIterableAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;
import org.assertj.core.api.ObjectAssertFactory;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Lists.newArrayList;

public class ConditionEventsAssertion
        extends AbstractIterableAssert<ConditionEventsAssertion, ConditionEvents, ConditionEvent, ObjectAssert<ConditionEvent>> {

    public ConditionEventsAssertion(ConditionEvents actual) {
        super(actual, ConditionEventsAssertion.class);
    }

    public void containViolations(String violation, String... additional) {
        Assertions.assertThat(actual.containViolation()).as("Condition is violated").isTrue();

        List<String> expected = concat(violation, additional);
        if (!sorted(messagesOf(actual.getViolating())).equals(sorted(expected))) {
            failWithMessage("Expected %s to contain only violations %s", actual, expected);
        }
    }

    public void containAllowed(String message, String... additional) {
        Assertions.assertThat(actual.getAllowed().size()).as("Allowed events occurred").isGreaterThan(0);

        List<String> expected = concat(message, additional);
        if (!sorted(messagesOf(actual.getAllowed())).equals(sorted(expected))) {
            failWithMessage("Expected %s to contain only allowed events %s", actual, expected);
        }
    }

    private List<String> messagesOf(Collection<? extends ConditionEvent> events) {
        final List<String> result = new ArrayList<>();
        for (ConditionEvent event : events) {
            result.addAll(event.getDescriptionLines());
        }
        return result;
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
        Assertions.assertThat(actual.containViolation()).as("Condition is violated").isFalse();
        Assertions.assertThat(messagesOf(actual.getViolating())).as("Violating messages").isEmpty();
    }

    public ConditionEventsAssertion haveOneViolationMessageContaining(String... messageParts) {
        return haveOneViolationMessageContaining(ImmutableSet.copyOf(messageParts));
    }

    public ConditionEventsAssertion haveOneViolationMessageContaining(Set<String> messageParts) {
        Assertions.assertThat(messagesOf(actual.getViolating())).as("Number of violations").hasSize(1);
        AbstractCharSequenceAssert<?, String> assertion = Assertions.assertThat(getOnlyElement(messagesOf(actual.getViolating())));
        for (String part : messageParts) {
            assertion.as("Violation message").contains(part);
        }
        return this;
    }

    public ConditionEventsAssertion haveAtLeastOneViolationMessageMatching(String regex) {
        Pattern pattern = Pattern.compile(regex, Pattern.DOTALL);
        for (String message : messagesOf(actual.getViolating())) {
            if (pattern.matcher(message).matches()) {
                return this;
            }
        }
        throw new AssertionError(String.format("No message matches pattern '%s'", regex));
    }

    @Override
    protected ObjectAssert<ConditionEvent> toAssert(ConditionEvent value, String description) {
        return new ObjectAssertFactory<ConditionEvent>().createAssert(value).as(description);
    }
}
