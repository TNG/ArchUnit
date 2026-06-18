package com.tngtech.archunit.testutils;

import java.util.List;

import com.tngtech.archunit.testutils.ExpectedTestFailures.TestFailure;
import org.junit.runner.JUnitCore;

import static java.util.stream.Collectors.toList;

public class ExpectedJUnit4TestFailures  {
    public static ExpectedTestFailures forTests(Class<?>... testClasses) {
        return ExpectedTestFailures.forTests(ExpectedJUnit4TestFailures::runTests, testClasses);
    }

    private static List<TestFailure> runTests(Class<?> testClass) {
        return new JUnitCore().run(testClass).getFailures().stream()
                .map(failure -> new TestFailure(failure.getDescription().getMethodName(), failure.getException()))
                .collect(toList());
    }
}
