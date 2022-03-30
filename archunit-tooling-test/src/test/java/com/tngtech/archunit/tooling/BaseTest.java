package com.tngtech.archunit.tooling;

import java.util.Collections;
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
        TestReport report = engine.execute(Collections.singleton(testFile));
        ExecutedTestFile actual = findResult(report, testFile.getFixture());

        // then
        assertThat(actual.getResult("shouldReportSuccess").get()).isEqualTo(SUCCESS);
        assertThat(actual.getResult("shouldReportFailure").get()).isEqualTo(FAILURE);
        assertThat(actual.getResult("shouldReportError").get()).isEqualTo(engine.reportsErrors() ? ERROR : FAILURE);
        assertThat(actual.getResult("shouldBeSkipped").get()).isEqualTo(SKIPPED);
    }

    void shouldOnlyExecuteSelectedTests(TestEngine engine, Class<?> fixture) throws Exception {
        // given
        TestFile testFile = new TestFile(fixture, "shouldReportSuccess");

        // when
        TestReport report = engine.execute(Collections.singleton(testFile));
        ExecutedTestFile actual = findResult(report, testFile.getFixture());

        // then
        assertThat(actual.getResult("shouldReportSuccess").isPresent()).isTrue();
        assertThat(actual.getResults()).hasSize(1);
    }

    void shouldConditionallyIgnoreTest(TestEngine engine, Class<?> fixture, Map.Entry<String, ExecutedTestFile.TestResult> resultForEnvVar) throws Exception {
        // given
        String envValue = resultForEnvVar.getKey();
        ExecutedTestFile.TestResult result = resultForEnvVar.getValue();
        TestFile testFile = new TestFile(fixture, "shouldBeSkippedConditionally");

        // when
        ExecutedTestFile actual = withEnvironmentVariable("SKIP_BY_ENV_VARIABLE", envValue).execute(() -> {
            TestReport report = engine.execute(Collections.singleton(testFile));
            return findResult(report, fixture);
        });

        // then
        assertThat(actual.getResult("shouldBeSkippedConditionally").get()).isEqualTo(result);
    }

    private ExecutedTestFile findResult(TestReport report, Class<?> fixture) {
        return report.getFile(fixture.getName()).get();
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
