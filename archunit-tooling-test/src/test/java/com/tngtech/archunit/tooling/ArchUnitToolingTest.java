package com.tngtech.archunit.tooling;

import java.util.Map;
import java.util.stream.Stream;

import com.tngtech.archunit.tooling.engines.jupiter.JUnitJupiterEngine;
import com.tngtech.archunit.tooling.examples.ArchJUnit4SuiteTest;
import com.tngtech.archunit.tooling.examples.ArchJUnit4Test;
import com.tngtech.archunit.tooling.examples.ArchJUnit5SuiteTest;
import com.tngtech.archunit.tooling.examples.ArchJUnit5Test;
import org.junit.jupiter.api.Disabled;
import org.junitpioneer.jupiter.cartesian.ArgumentSets;
import org.junitpioneer.jupiter.cartesian.CartesianTest;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class ArchUnitToolingTest extends BaseTest {

    @CartesianTest
    @CartesianTest.MethodFactory("enginesAndFixtures")
    void shouldReportCorrectTestResults(TestEngine engine, Class<?> fixture) throws Exception {
        super.shouldReportCorrectTestResults(engine, fixture);
    }

    @CartesianTest
    @CartesianTest.MethodFactory("enginesAndFixtures")
    void shouldOnlyExecuteSelectedTests(TestEngine engine, Class<?> fixture) throws Exception {
        assumeTrue(engine instanceof JUnitJupiterEngine);
        super.shouldOnlyExecuteSelectedTests(engine, fixture);
    }

    @CartesianTest
    @CartesianTest.MethodFactory("enginesFixturesAndIgnoreEnvVariables")
    @Disabled
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
                ArchJUnit5Test.class,
                ArchJUnit4SuiteTest.class,
                ArchJUnit5SuiteTest.class
        );
    }
}
