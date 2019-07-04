package com.tngtech.archunit.lang;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.base.Predicate;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.assertj.core.api.Assertions.assertThat;

public class EvaluationResultTest {
    @Test
    public void properties_are_passed_to_FailureReport() {
        EvaluationResult result = new EvaluationResult(
                hasDescription("special description"),
                events("first bummer", "second bummer"),
                Priority.HIGH);

        assertThat(result.getFailureReport().getDetails()).containsExactly("first bummer", "second bummer");
        assertThat(result.getFailureReport().toString())
                .containsPattern("Priority.*HIGH")
                .contains("special description")
                .contains("first bummer")
                .contains("second bummer");
    }

    @Test
    public void allows_clients_to_handle_violations() {
        EvaluationResult result = evaluationResultWith(
                new SimpleConditionEvent(ImmutableSet.of("message"), false, "expected"),
                new SimpleConditionEvent(ImmutableSet.of("other message"), true, "not expected"),
                new SimpleConditionEvent(ImmutableList.of("yet another message"), false, "not expected"),
                new SimpleConditionEvent(ImmutableSet.of("second message"), false, "also expected"));

        final Set<String> actual = new HashSet<>();
        result.handleViolations(new ViolationHandler<Set<?>>() {
            @Override
            public void handle(Collection<Set<?>> violatingObject, String message) {
                actual.add(getOnlyElement(getOnlyElement(violatingObject)) + ": " + message);
            }
        });

        assertThat(actual).containsOnly("message: expected", "second message: also expected");
    }

    @Test
    public void can_filter_lines() {
        EvaluationResult result = evaluationResultWith(
                new TestEvent(true, "keep first line1", "keep second line1"),
                new TestEvent(true, "drop first line2", "keep second line2"),
                new TestEvent(true, "drop first line3", "drop second line3"),
                new TestEvent(false, "keep first line4", "keep second line4"));

        EvaluationResult filtered = result.filterDescriptionsMatching(new Predicate<String>() {
            @Override
            public boolean apply(String input) {
                return input.contains("keep");
            }
        });

        assertThat(filtered.hasViolation()).as("filtered has violation").isTrue();
        assertThat(filtered.getFailureReport().getDetails()).containsOnly("keep first line1", "keep second line1", "keep second line2");
    }

    private EvaluationResult evaluationResultWith(ConditionEvent... events) {
        return new EvaluationResult(hasDescription("unimportant"), events(events), Priority.MEDIUM);
    }

    private ConditionEvents events(String... messages) {
        Set<ConditionEvent> events = new HashSet<>();
        for (String message : messages) {
            events.add(new SimpleConditionEvent(new Object(), false, message));
        }
        return events(events.toArray(new ConditionEvent[0]));
    }

    private ConditionEvents events(ConditionEvent... events) {
        ConditionEvents result = new ConditionEvents();
        for (ConditionEvent event : events) {
            result.add(event);
        }
        return result;
    }

    private HasDescription hasDescription(final String description) {
        return new HasDescription() {
            @Override
            public String getDescription() {
                return description;
            }
        };
    }

    private static class TestEvent implements ConditionEvent {
        private boolean violation;
        private List<String> descriptionLines;

        private TestEvent(boolean violation, String... descriptionLines) {
            this.violation = violation;
            this.descriptionLines = ImmutableList.copyOf(descriptionLines);
        }

        @Override
        public boolean isViolation() {
            return violation;
        }

        @Override
        public void addInvertedTo(ConditionEvents events) {
            throw new UnsupportedOperationException("Implement me");
        }

        @Override
        public void describeTo(CollectsLines messages) {
            throw new UnsupportedOperationException("Obsolete");
        }

        @Override
        public List<String> getDescriptionLines() {
            return descriptionLines;
        }

        @Override
        public void handleWith(Handler handler) {
            throw new UnsupportedOperationException("Implement me");
        }
    }
}