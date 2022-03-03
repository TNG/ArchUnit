package com.tngtech.archunit.tooling;

import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.tooling.engines.jupiter.JUnitJupiterEngine;
import com.tngtech.archunit.tooling.engines.surefire.MavenSurefireEngine;
import com.tngtech.archunit.tooling.examples.RegularJunit4Test;
import com.tngtech.archunit.tooling.examples.RegularJunit5Test;
import org.junitpioneer.jupiter.cartesian.ArgumentSets;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.MethodFactory;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static com.github.stefanbirkner.systemlambda.SystemLambda.withEnvironmentVariable;
import static com.tngtech.archunit.tooling.ExecutedTestFile.TestResult.*;
import static org.assertj.core.api.Assertions.assertThat;

public class BaselineTest {

    @CartesianTest
    @MethodFactory("enginesAndFixtures")
    void shouldReportCorrectTestResults(TestEngine engine, Class<?> fixture) throws Exception {
        // given
        TestFile testFile = new TestFile(fixture);

        // when
        TestReport report = engine.execute(Collections.singleton(testFile));
        ExecutedTestFile actual = report.getFile(testFile.getFixture()).get();

        // then
        assertThat(actual.getResult("shouldReportSuccess")).isEqualTo(SUCCESS);
        assertThat(actual.getResult("shouldReportFailure")).isEqualTo(FAILURE);
        assertThat(actual.getResult("shouldReportError")).isEqualTo(engine.reportsErrors() ? ERROR : FAILURE);
        assertThat(actual.getResult("shouldBeSkipped")).isEqualTo(SKIPPED);
    }

    @CartesianTest
    @MethodFactory("enginesAndFixtures")
    void shouldOnlyExecuteSelectedTests(TestEngine engine, Class<?> fixture) throws Exception {
        // given
        TestFile testFile = new TestFile(fixture, "shouldReportSuccess");

        // when
        TestReport report = engine.execute(Collections.singleton(testFile));
        ExecutedTestFile actual = report.getFile(testFile.getFixture()).get();

        // then
        assertThat(actual.getResults()).hasSize(1);
    }

    @CartesianTest
    @MethodFactory("enginesFixturesAndIgnoreEnvVariables")
    void shouldConditionallyIgnoreTest(TestEngine engine, Class<?> fixture, Map.Entry<String, ExecutedTestFile.TestResult> resultForEnvVar) throws Exception {
        // given
        String envValue = resultForEnvVar.getKey();
        ExecutedTestFile.TestResult result = resultForEnvVar.getValue();
        TestFile testFile = new TestFile(fixture, "shouldBeSkippedConditionally");

        // when
        ExecutedTestFile actual = withEnvironmentVariable("SKIP_BY_ENV_VARIABLE", envValue).execute(() -> {
            TestReport report = engine.execute(Collections.singleton(testFile));
            return report.getFile(fixture).get();
        });

        // then
        assertThat(actual.getResult("shouldBeSkippedConditionally")).isEqualTo(result);
    }

    static ArgumentSets enginesFixturesAndIgnoreEnvVariables() {
        return enginesAndFixtures()
                .argumentsForNextParameter(expectedResultPerEnvVariableValue().entrySet());
    }

    static Map<String, ExecutedTestFile.TestResult> expectedResultPerEnvVariableValue() {
        return ImmutableMap.<String, ExecutedTestFile.TestResult>builder()
                .put("true", SKIPPED)
                .put("false", SUCCESS)
                .build();
    }

    static ArgumentSets enginesAndFixtures() {
        return ArgumentSets
                .argumentsForFirstParameter(engines())
                .argumentsForNextParameter(fixtures());
    }

    static Stream<TestEngine> engines() {
        return Stream.of(
                JUnitJupiterEngine.INSTANCE,
                MavenSurefireEngine.INSTANCE
        );
    }

    static Stream<Class<?>> fixtures() {
        return Stream.of(
                RegularJunit5Test.class,
                RegularJunit4Test.class);
    }
}
