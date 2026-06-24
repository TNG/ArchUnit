package com.tngtech.archunit.testutils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class ExpectedJUnit5TestFailures extends ExpectedTestFailures<DynamicTest> {

    ExpectedJUnit5TestFailures(Class<?>[] testClasses) {
        super(testClasses);
    }

    @Override
    public Stream<DynamicTest> toDynamicTests(Consumer<Runnable> aroundTestInvoke) {
        return testClasses.stream()
                .map(testClass -> new RunnableTest(ExpectedJUnit5TestFailures::runTests, testClass))
                .map(test -> dynamicTest(
                        test.getDisplayName(),
                        () -> aroundTestInvoke.accept(() -> assertActualAndExpectedViolationsMatch(test))));
    }

    public static ExpectedJUnit5TestFailures forTests(Class<?>... testClasses) {
        return new ExpectedJUnit5TestFailures(testClasses);
    }

    private static List<TestFailure> runTests(Class<?> testClass) {
        List<TestFailure> result = new ArrayList<>();

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(testClass))
                .build();
        Launcher launcher = LauncherFactory.create();
        launcher.registerTestExecutionListeners(new TestExecutionListener() {
            @Override
            public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
                if (!testIdentifier.isContainer() && testExecutionResult.getStatus() == TestExecutionResult.Status.FAILED) {
                    testExecutionResult.getThrowable().ifPresent(throwable -> {
                        result.add(new TestFailure(testIdentifier.getDisplayName(), throwable));
                    });
                }
            }
        });
        launcher.execute(request);

        return result;
    }
}
