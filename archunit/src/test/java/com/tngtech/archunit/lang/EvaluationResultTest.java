package com.tngtech.archunit.lang;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.tngtech.archunit.base.HasDescription;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.lang.Priority.MEDIUM;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

public class EvaluationResultTest {

    @Test
    public void reports_description_lines_of_events() {
        List<String> expectedLinesAlphabetically = ImmutableList.of("another event message", "some event message");

        EvaluationResult result = new EvaluationResult(
                hasDescription("irrelevant"),
                events(expectedLinesAlphabetically.get(1), expectedLinesAlphabetically.get(0)),
                MEDIUM);

        assertThat(result.getFailureReport().getDetails()).containsExactlyElementsOf(expectedLinesAlphabetically);
    }

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
    @SuppressWarnings("Convert2Lambda") // to retrieve the type information ViolationHandler may not be converted to a Lambda
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

        EvaluationResult filtered = result.filterDescriptionsMatching(input -> input.contains("keep"));

        assertThat(filtered.hasViolation()).as("filtered has violation").isTrue();
        assertThat(filtered.getFailureReport().getDetails()).containsOnly("keep first line1", "keep second line1", "keep second line2");
    }

    @Test
    @SuppressWarnings("Convert2Lambda") // We need the reified type here, so we cannot convert it to a lambda
    public void handleViolations_reports_only_violations_referring_to_the_correct_type() {
        EvaluationResult result = evaluationResultWith(
                SimpleConditionEvent.satisfied(new CorrectType("do not handle"), "I'm not violated"),
                SimpleConditionEvent.violated(new WrongType(), "I'm violated, but wrong type"),
                SimpleConditionEvent.violated(new WrongSupertype(), "I'm violated, but wrong type"),
                SimpleConditionEvent.violated(new CorrectType("handle type"), "I'm violated and correct type"),
                SimpleConditionEvent.violated(new CorrectSubtype("handle sub type"), "I'm violated and correct sub type"));

        final Set<String> handledFailures = new HashSet<>();
        result.handleViolations(new ViolationHandler<CorrectType>() {
            @Override
            public void handle(Collection<CorrectType> violatingObjects, String message) {
                handledFailures.add(Joiner.on(", ").join(violatingObjects) + ": " + message);
            }
        });

        assertThat(handledFailures).containsOnly(
                "handle type: I'm violated and correct type",
                "handle sub type: I'm violated and correct sub type");
    }

    @Test
    public void handles_erased_generics_as_upper_bound() {
        EvaluationResult result = evaluationResultWith(
                SimpleConditionEvent.violated(new CorrectType("ignore"), "correct"),
                SimpleConditionEvent.violated(new WrongType(), "wrong"));

        Set<String> handledFailureMessages = new HashSet<>();
        result.handleViolations(genericBoundByCorrectType(handledFailureMessages));

        assertThat(handledFailureMessages).containsOnly("correct");

        handledFailureMessages = new HashSet<>();
        result.handleViolations(unboundGeneric(handledFailureMessages));

        assertThat(handledFailureMessages).containsOnly("correct", "wrong");
    }

    @SuppressWarnings("Convert2Lambda") // to retrieve the type information ViolationHandler may not be converted to a Lambda
    private <T extends CorrectType> ViolationHandler<?> genericBoundByCorrectType(final Set<String> handledFailureMessages) {
        return new ViolationHandler<T>() {
            @Override
            public void handle(Collection<T> violatingObjects, String message) {
                handledFailureMessages.add(message);
            }
        };
    }

    @SuppressWarnings("Convert2Lambda") // to retrieve the type information ViolationHandler may not be converted to a Lambda
    private <T> ViolationHandler<?> unboundGeneric(final Set<String> handledFailureMessages) {
        return new ViolationHandler<T>() {
            @Override
            public void handle(Collection<T> violatingObjects, String message) {
                handledFailureMessages.add(message);
            }
        };
    }

    @Test
    public void can_handle_with_generic_superclasses() {
        EvaluationResult result = evaluationResultWith(
                SimpleConditionEvent.violated(new Object(), "ignore"),
                SimpleConditionEvent.violated("correct", "ignore"));

        StringHandler handler = new StringHandler();
        result.handleViolations(handler);

        assertThat(handler.getRecorded()).containsOnly("correct");
    }

    private EvaluationResult evaluationResultWith(ConditionEvent... events) {
        return new EvaluationResult(hasDescription("unimportant"), events(events), MEDIUM);
    }

    private ConditionEvents events(String... messages) {
        return events(stream(messages)
                .map(message -> new SimpleConditionEvent(new Object(), false, message))
                .distinct()
                .toArray(ConditionEvent[]::new));
    }

    private ConditionEvents events(ConditionEvent... events) {
        ConditionEvents result = new ConditionEvents();
        for (ConditionEvent event : events) {
            result.add(event);
        }
        return result;
    }

    private HasDescription hasDescription(final String description) {
        return () -> description;
    }


    private static class BaseHandler<T> implements ViolationHandler<T> {
        private final List<T> recorded = new ArrayList<>();

        @Override
        public void handle(Collection<T> violatingObjects, String message) {
            recorded.add(Iterables.getOnlyElement(violatingObjects));
        }

        List<T> getRecorded() {
            return recorded;
        }
    }

    private static class StringHandler extends BaseHandler<String> {
    }

    private static class CorrectSubtype extends CorrectType {
        CorrectSubtype(String message) {
            super(message);
        }
    }

    static class CorrectType extends WrongSupertype {
        String message;

        CorrectType(String message) {
            this.message = message;
        }

        @Override
        public String toString() {
            return message;
        }
    }

    private static class WrongType {
    }

    private static class WrongSupertype {
    }

    private static class TestEvent implements ConditionEvent {
        private final boolean violation;
        private final List<String> descriptionLines;

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
        public List<String> getDescriptionLines() {
            return descriptionLines;
        }

        @Override
        public void handleWith(Handler handler) {
            throw new UnsupportedOperationException("Implement me");
        }
    }
}
