package com.tngtech.archunit.tooling;

import com.tngtech.archunit.tooling.examples.RegularJUnit4Test;
import com.tngtech.archunit.tooling.examples.RegularJUnit5Test;
import org.junitpioneer.jupiter.cartesian.ArgumentSets;
import org.junitpioneer.jupiter.cartesian.CartesianTest;
import org.junitpioneer.jupiter.cartesian.CartesianTest.MethodFactory;

import java.util.Map;
import java.util.stream.Stream;

public class BaselineTest extends BaseTest {

    @CartesianTest
    @MethodFactory("enginesAndFixtures")
    void shouldReportCorrectTestResults(TestEngine engine, Class<?> fixture) throws Exception {
        super.shouldReportCorrectTestResults(engine, fixture);
    }

    @CartesianTest
    @MethodFactory("enginesAndFixtures")
    void shouldOnlyExecuteSelectedTests(TestEngine engine, Class<?> fixture) throws Exception {
        super.shouldOnlyExecuteSelectedTests(engine, fixture);
    }

    @CartesianTest
    @MethodFactory("enginesFixturesAndIgnoreEnvVariables")
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
                RegularJUnit5Test.class,
                RegularJUnit4Test.class);
    }
}
