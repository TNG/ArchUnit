package com.tngtech.archunit.tooling.engines.jupiter;

import com.tngtech.archunit.junit.FieldSource;
import com.tngtech.archunit.tooling.ExecutedTestFile.TestResult;
import com.tngtech.archunit.tooling.TestEngine;
import com.tngtech.archunit.tooling.TestFile;
import com.tngtech.archunit.tooling.TestReport;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.tngtech.archunit.junit.FieldSelector.selectField;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

public enum JUnitJupiterEngine implements TestEngine {
    INSTANCE;

    private final Launcher launcher = LauncherFactory.create();

    @Override
    public TestReport execute(Set<TestFile> testFiles) {
        LauncherDiscoveryRequest request = toDiscoveryRequest(testFiles);
        TestExecutionCollector testExecutionCollector = new TestExecutionCollector();
        launcher.execute(launcher.discover(request), testExecutionCollector);
        return testExecutionCollector.getReport();
    }

    private LauncherDiscoveryRequest toDiscoveryRequest(Set<TestFile> testFiles) {
        return LauncherDiscoveryRequestBuilder.request()
                .selectors(testFiles.stream()
                        .flatMap(this::toSelectors)
                        .collect(Collectors.toList()))
                .filters()
                .build();
    }

    private Stream<DiscoverySelector> toSelectors(TestFile testFile) {
        if (testFile.hasTestCasesFilter()) {
            return testFile.getTestCases().stream().map(testCase -> toSelector(testFile.getFixture(), testCase));
        }
        return Stream.of(selectClass(testFile.getFixture()));
    }

    private DiscoverySelector toSelector(Class<?> fixture, String testCase) {
        return getMethodByName(fixture, testCase)
                .<DiscoverySelector>map(method -> selectMethod(fixture, method))
                .orElseGet(() -> getFieldByName(fixture, testCase)
                        .<DiscoverySelector>map(field -> selectField(fixture, field.getName()))
                        .orElseThrow(RuntimeException::new));
    }

    private Optional<Method> getMethodByName(Class<?> owner, String name) {
        return Arrays.stream(owner.getDeclaredMethods())
                .filter(method -> name.equals(method.getName()))
                .findFirst();
    }

    private Optional<Field> getFieldByName(Class<?> owner, String name) {
        try {
            return Optional.of(owner.getDeclaredField(name));
        } catch (NoSuchFieldException e) {
            return Optional.empty();
        }
    }

    private static class TestExecutionCollector implements TestExecutionListener {

        private final TestReport report = new TestReport();

        public TestReport getReport() {
            return report;
        }

        @Override
        public void executionSkipped(TestIdentifier testIdentifier, String reason) {
            registerTestResult(testIdentifier, TestResult.SKIPPED);
        }

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            registerTestResult(testIdentifier, testExecutionResult);
        }

        private void registerTestResult(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            if (testIdentifier.isContainer()) {
                testExecutionResult.getThrowable().ifPresent(throwable -> { throw new RuntimeException(throwable); });
                return;
            }
            registerTestResult(testIdentifier, toResult(testExecutionResult.getStatus()));
        }

        private void registerTestResult(TestIdentifier testIdentifier, TestResult testResult) {
            Map.Entry<Class<?>, String> testCaseEntry = testIdentifier.getSource()
                    .map(source -> resolveTestCaseInfo(testIdentifier, source))
                    .orElseThrow(RuntimeException::new);
            report.ensureFileForFixture(testCaseEntry.getKey()).addResult(testCaseEntry.getValue(), testResult);
        }

        private TestResult toResult(TestExecutionResult.Status status) {
            switch (status) {
                case SUCCESSFUL:
                    return TestResult.SUCCESS;
                case FAILED:
                    return TestResult.FAILURE;
                case ABORTED:
                    return TestResult.SKIPPED;
                default:
                    throw new RuntimeException("Unrecognized enum value TestExecutionResult.Status#" + status);
            }
        }

        private Map.Entry<Class<?>, String> resolveTestCaseInfo(TestIdentifier testIdentifier, TestSource source) {
            Class<?> owner;
            String testCase;
            if (source instanceof MethodSource) {
                owner = ((MethodSource) source).getJavaClass();
                testCase = ((MethodSource) source).getMethodName();
            } else if (source instanceof ClassSource) {
                owner = ((ClassSource) source).getJavaClass();
                testCase = testIdentifier.getDisplayName();
            } else if (source instanceof FieldSource) {
                owner = ((FieldSource) source).getJavaClass();
                testCase = ((FieldSource) source).getFieldName();
            } else {
                throw new RuntimeException("Unrecognized TestSource " + source.getClass().getSimpleName());
            }
            return Map.entry(owner, testCase);
        }

    }

    @Override
    public String toString() {
        return "JUnit 5 Jupiter";
    }
}
