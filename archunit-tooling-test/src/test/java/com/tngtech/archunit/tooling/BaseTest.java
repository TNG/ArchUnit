package com.tngtech.archunit.tooling;

import java.util.Map;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.tooling.engines.gradle.GradleEngine;
import com.tngtech.archunit.tooling.engines.jupiter.JUnitJupiterEngine;
import com.tngtech.archunit.tooling.engines.surefire.MavenSurefireEngine;
import org.junitpioneer.jupiter.cartesian.ArgumentSets;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static com.tngtech.archunit.tooling.ExecutedTestFile.TestResult.ERROR;
import static com.tngtech.archunit.tooling.ExecutedTestFile.TestResult.FAILURE;
import static com.tngtech.archunit.tooling.ExecutedTestFile.TestResult.SKIPPED;
import static com.tngtech.archunit.tooling.ExecutedTestFile.TestResult.SUCCESS;
import static org.assertj.core.api.Assertions.assertThat;

public abstract class BaseTest {

    void shouldReportCorrectTestResults(TestEngine engine, Class<?> fixture) throws Exception {
        // given
        TestFile testFile = new TestFile(fixture);

        // when
        TestReport report = engine.execute(testFile);
        TestResults actual = TestResults.of(report, testFile);

        // then
        assertThat(actual.hasInitializationError()).isFalse();
        assertThat(actual.getResult("shouldReportSuccess")).isEqualTo(SUCCESS);
        assertThat(actual.getResult("shouldReportFailure")).isEqualTo(FAILURE);
        assertThat(actual.getResult("shouldReportError")).isEqualTo(engine.reportsErrors() ? ERROR : FAILURE);
        assertThat(actual.getResult("shouldBeSkipped")).isEqualTo(SKIPPED);
    }

    void shouldOnlyExecuteSelectedTests(TestEngine engine, Class<?> fixture) throws Exception {
        // given
        TestFile testFile = new TestFile(fixture, "shouldReportSuccess");

        // when
        TestReport report = engine.execute(testFile);
        TestResults actual = TestResults.of(report, testFile);

        // then
        assertThat(actual.hasInitializationError()).isFalse();
        assertThat(actual.hasResult("shouldReportSuccess")).isTrue();
        assertThat(actual.resultCount()).isEqualTo(1);
    }

    void shouldConditionallyIgnoreTest(TestEngine engine, Class<?> fixture, Map.Entry<String, ExecutedTestFile.TestResult> resultForEnvVar) throws Exception {
        // given
        String envValue = resultForEnvVar.getKey();
        ExecutedTestFile.TestResult result = resultForEnvVar.getValue();
        TestFile testFile = new TestFile(fixture, "shouldBeSkippedConditionally");

        // when
        TestResults actual = withEnvironmentVariable("SKIP_BY_ENV_VARIABLE", envValue).execute(() -> {
            TestReport report = engine.execute(testFile);
            return TestResults.of(report, testFile);
        });

        // then
        assertThat(actual.hasInitializationError()).isFalse();
        assertThat(actual.getResult("shouldBeSkippedConditionally")).isEqualTo(result);
    }

    @SuppressWarnings("unused")
    static ArgumentSets enginesFixturesAndIgnoreEnvVariables(Stream<Class<?>> fixtures) {
        return enginesAndFixtures(fixtures)
                .argumentsForNextParameter(expectedResultPerEnvVariableValue().entrySet());
    }

    static Map<String, ExecutedTestFile.TestResult> expectedResultPerEnvVariableValue() {
        return ImmutableMap.<String, ExecutedTestFile.TestResult>builder()
                .put("true", SKIPPED)
                .put("false", SUCCESS)
                .build();
    }

    static ArgumentSets enginesAndFixtures(Stream<Class<?>> fixtures) {
        return ArgumentSets
                .argumentsForFirstParameter(engines())
                .argumentsForNextParameter(fixtures);
    }

    static Stream<TestEngine> engines() {
        return Stream.of(
                JUnitJupiterEngine.INSTANCE,
                MavenSurefireEngine.FOR_TESTS_LOCATED_IN_EXAMPLES,
                GradleEngine.FOR_TESTS_LOCATED_IN_EXAMPLES
        );
    }
}
