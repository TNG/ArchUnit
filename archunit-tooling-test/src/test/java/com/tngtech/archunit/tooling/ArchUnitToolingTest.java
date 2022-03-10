package com.tngtech.archunit.tooling;

import com.tngtech.archunit.tooling.examples.ArchJUnit4Test;
import com.tngtech.archunit.tooling.examples.ArchJUnit5Test;
import org.junitpioneer.jupiter.cartesian.ArgumentSets;
import org.junitpioneer.jupiter.cartesian.CartesianTest;

import java.util.Map;
import java.util.stream.Stream;

public class ArchUnitToolingTest extends BaseTest {

    @CartesianTest
    @CartesianTest.MethodFactory("enginesAndFixtures")
    void shouldReportCorrectTestResults(TestEngine engine, Class<?> fixture) throws Exception {
        super.shouldReportCorrectTestResults(engine, fixture);
    }

    @CartesianTest
    @CartesianTest.MethodFactory("enginesAndFixtures")
    void shouldOnlyExecuteSelectedTests(TestEngine engine, Class<?> fixture) throws Exception {
        super.shouldOnlyExecuteSelectedTests(engine, fixture);
    }

    @CartesianTest
    @CartesianTest.MethodFactory("enginesFixturesAndIgnoreEnvVariables")
    void shouldConditionallyIgnoreTest(TestEngine engine, Class<?> fixture, Map.Entry<String, ExecutedTestFile.TestResult> resultForEnvVar) throws Exception {
        super.shouldConditionallyIgnoreTest(engine, fixture, resultForEnvVar);
    }

    @SuppressWarnings("unused")
    static ArgumentSets enginesFixturesAndIgnoreEnvVariables() {
        return enginesAndFixtures()
                .argumentsForNextParameter(expectedResultPerEnvVariableValue().entrySet());
    }

    static ArgumentSets enginesAndFixtures() {
        return enginesAndFixtures(fixtures());
    }

    static Stream<Class<?>> fixtures() {
        return Stream.of(
                ArchJUnit4Test.class,
                ArchJUnit5Test.class
        );
    }
}
