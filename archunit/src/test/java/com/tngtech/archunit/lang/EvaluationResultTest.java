package com.tngtech.archunit.lang;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.HasDescription;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.lang.EvaluationResult.ARCHUNIT_IGNORE_PATTERNS_FILE_NAME;
import static com.tngtech.archunit.lang.Priority.MEDIUM;
import static com.tngtech.archunit.testutil.TestUtils.toUri;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.util.Arrays.stream;
import static org.assertj.core.api.Assertions.assertThat;

public class EvaluationResultTest {

    @Before
    public void setUp() throws IOException {
        cleanIgnoreFile();
    }

    @After
    public void tearDown() throws IOException {
        cleanIgnoreFile();
    }

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
    public void allows_clients_to_handle_violations() {
        EvaluationResult result = evaluationResultWith(
                new SimpleConditionEvent(ImmutableSet.of("message"), false, "expected"),
                new SimpleConditionEvent(ImmutableSet.of("other message"), true, "not expected"),
                new SimpleConditionEvent(ImmutableList.of("yet another message"), false, "not expected"),
                new SimpleConditionEvent(ImmutableSet.of("second message"), false, "also expected"));

        final Set<String> actual = new HashSet<>();
        result.handleViolations((Collection<Set<String>> violatingObject, String message) ->
                actual.add(getOnlyElement(getOnlyElement(violatingObject)) + ": " + message));

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
    public void filtering_lines_resets_information_about_number_of_violations() {
        ConditionEvents events = events(new TestEvent(true, "drop first line1", "keep second line1"));
        String informationAboutNumberOfViolations = "test number";
        events.setInformationAboutNumberOfViolations(informationAboutNumberOfViolations);
        EvaluationResult result = new EvaluationResult(hasDescription("unimportant"), events, MEDIUM);

        EvaluationResult filtered = result.filterDescriptionsMatching(input -> input.contains("keep"));

        assertThat(filtered.getFailureReport().toString()).contains("(1 times)");
        assertThat(filtered.getFailureReport().toString()).doesNotContain(informationAboutNumberOfViolations);
        assertThat(filtered.getFailureReport().getDetails()).containsOnly("keep second line1");
    }

    @Test
    public void handleViolations_reports_only_violations_referring_to_the_correct_type() {
        EvaluationResult result = evaluationResultWith(
                SimpleConditionEvent.satisfied(new CorrectType("do not handle"), "I'm not violated"),
                SimpleConditionEvent.violated(new WrongType(), "I'm violated, but wrong type"),
                SimpleConditionEvent.violated(new WrongSupertype(), "I'm violated, but wrong type"),
                SimpleConditionEvent.violated(new CorrectType("handle type"), "I'm violated and correct type"),
                SimpleConditionEvent.violated(new CorrectSubtype("handle sub type"), "I'm violated and correct sub type"));

        final Set<String> handledFailures = new HashSet<>();
        result.handleViolations((Collection<CorrectType> violatingObjects, String message) ->
                handledFailures.add(Joiner.on(", ").join(violatingObjects) + ": " + message));

        assertThat(handledFailures).containsOnly(
                "handle type: I'm violated and correct type",
                "handle sub type: I'm violated and correct sub type");
    }

    @Test
    public void filters_violations_ignored_by_archunit_ignore_patterns() throws IOException {
        writeIgnoreFileWithPatterns(".* one", ".*two");

        EvaluationResult result = evaluationResultWith(events("first one", "second two", "third one more", "fourth"));

        assertThat(result.hasViolation()).as("result has violation").isTrue();
        assertThat(result.getFailureReport().getDetails()).containsOnly("third one more", "fourth");
    }

    @Test
    public void if_all_messages_are_ignored_the_test_passes() throws IOException {
        writeIgnoreFileWithPatterns(".*");

        EvaluationResult result = evaluationResultWith(events("first one", "second two"));

        assertThat(result.hasViolation()).as("result has violation").isFalse();
    }

    @Test
    public void ignored_pattern_with_comment() throws IOException {
        writeIgnoreFileWithPatterns("# comment1", "#comment2", "regular_reg_exp");

        EvaluationResult result = evaluationResultWith(events("# comment1", "#comment2", "regular_reg_exp"));

        assertThat(result.hasViolation()).as("result has violation").isTrue();
        assertThat(result.getFailureReport().getDetails()).containsOnly("# comment1", "#comment2");
    }

    @Test
    public void filters_violations_ignored_by_archunit_ignore_patterns_from_multiple_sources() throws IOException {
        writeIgnoreFileWithPatterns(".* one", ".*two");

        EvaluationResult result = new EvaluationResult(hasDescription("irrelevant"), MEDIUM);
        result.add(evaluationResultWith(events("first one", "keep1")));
        result.add(evaluationResultWith(events("second two", "keep2")));

        assertThat(result.hasViolation()).as("result has violation").isTrue();
        assertThat(result.getFailureReport().getDetails()).containsOnly("keep1", "keep2");
    }

    @Test
    public void ignores_filtered_violations_ignored_by_archunit_ignore_patterns_when_handling_errors() throws IOException {
        writeIgnoreFileWithPatterns(".* one", ".*two");

        EvaluationResult result = evaluationResultWith(events("first one", "second two", "third one more", "fourth"));

        List<String> handledViolations = new ArrayList<>();
        result.handleViolations((Collection<Object> irrelevant, String message) -> handledViolations.add(message));

        assertThat(handledViolations).containsOnly("third one more", "fourth");
    }

    private EvaluationResult evaluationResultWith(ConditionEvents events) {
        return evaluationResultWith(events.getViolating().toArray(new ConditionEvent[0]));
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
        ConditionEvents result = ConditionEvents.Factory.create();
        for (ConditionEvent event : events) {
            result.add(event);
        }
        return result;
    }

    private HasDescription hasDescription(final String description) {
        return () -> description;
    }

    public static Path writeIgnoreFileWithPatterns(String... patterns) throws IOException {
        Path ignoreFile = ignoreFile();
        Files.write(ignoreFile, Joiner.on("\n").join(patterns).getBytes(UTF_8));
        return ignoreFile;
    }

    @SuppressWarnings("ConstantConditions")
    private static Path ignoreFile() {
        return Paths.get(toUri(EvaluationResultTest.class.getResource("/"))).resolve(ARCHUNIT_IGNORE_PATTERNS_FILE_NAME);
    }

    private void cleanIgnoreFile() throws IOException {
        Path ignoreFile = ignoreFile();
        if (exists(ignoreFile)) {
            delete(ignoreFile);
        }
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
        public ConditionEvent invert() {
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
