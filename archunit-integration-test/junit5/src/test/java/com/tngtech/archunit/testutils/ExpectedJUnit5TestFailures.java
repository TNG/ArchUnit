package com.tngtech.archunit.testutils;

import java.util.ArrayList;
import java.util.List;

import com.tngtech.archunit.testutils.ExpectedTestFailures.TestFailure;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class ExpectedJUnit5TestFailures {
    public static ExpectedTestFailures forTests(Class<?>... testClasses) {
        return ExpectedTestFailures.forTests(ExpectedJUnit5TestFailures::runTests, testClasses);
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
