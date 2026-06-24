package com.tngtech.archunit.testutils;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.runner.JUnitCore;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class ExpectedJUnit4TestFailures extends ExpectedTestFailures<DynamicTest> {

    ExpectedJUnit4TestFailures(Class<?>[] testClasses) {
        super(testClasses);
    }

    @Override
    public Stream<DynamicTest> toDynamicTests(Consumer<Runnable> aroundTestInvoke) {
        return testClasses.stream()
                .map(testClass -> new RunnableTest(ExpectedJUnit4TestFailures::runTests, testClass))
                .map(test -> dynamicTest(
                        test.getDisplayName(),
                        () -> aroundTestInvoke.accept(() -> assertActualAndExpectedViolationsMatch(test))));
    }

    public static ExpectedJUnit4TestFailures forTests(Class<?>... testClasses) {
        return new ExpectedJUnit4TestFailures(testClasses);
    }

    private static List<ExpectedTestFailures.TestFailure> runTests(Class<?> testClass) {
        return new JUnitCore().run(testClass).getFailures().stream()
                .map(failure -> new TestFailure(failure.getDescription().getMethodName(), failure.getException()))
                .collect(toList());
    }
}
