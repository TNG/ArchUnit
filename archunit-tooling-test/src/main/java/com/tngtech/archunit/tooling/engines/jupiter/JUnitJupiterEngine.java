package com.tngtech.archunit.tooling.engines.jupiter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.tngtech.archunit.junit.engine_api.FieldSource;
import com.tngtech.archunit.tooling.ExecutedTestFile.TestResult;
import com.tngtech.archunit.tooling.TestEngine;
import com.tngtech.archunit.tooling.TestFile;
import com.tngtech.archunit.tooling.TestReport;
import com.tngtech.archunit.tooling.utils.JUnitEngineResolver;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.core.ServiceLoaderTestEngineRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tngtech.archunit.junit.engine_api.FieldSelector.selectField;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectMethod;

public enum JUnitJupiterEngine implements TestEngine {
    INSTANCE;

    private static final Logger LOG = LoggerFactory.getLogger(JUnitJupiterEngine.class);
    private static final String ARCHUNIT_INCLUDES_PARAMETER_NAME = "archunit.junit.includeTestsMatching";

    private final JUnitEngineResolver engineResolver = new JUnitEngineResolver();

    @Override
    public TestReport execute(TestFile testFile) {
        Launcher launcher = LauncherFactory.create(LauncherConfig.builder()
                        .enableTestEngineAutoRegistration(false)
                        .addTestEngines(manuallyLoadCorrectTestEngines(testFile))
                .build());
        LauncherDiscoveryRequest request = toDiscoveryRequest(testFile);
        TestExecutionCollector testExecutionCollector = new TestExecutionCollector();
        launcher.execute(launcher.discover(request), testExecutionCollector);
        return testExecutionCollector.getReport();
    }

    private org.junit.platform.engine.TestEngine[] manuallyLoadCorrectTestEngines(TestFile testFile) {
        List<String> engineIds = engineResolver.resolveJUnitEngines(testFile);
        Iterable<org.junit.platform.engine.TestEngine> testEngines = new ServiceLoaderTestEngineRegistry().loadTestEngines();
        return StreamSupport.stream(testEngines::spliterator, 0, false)
                .filter(engine -> engineIds.contains(engine.getId()))
                .toArray(org.junit.platform.engine.TestEngine[]::new);
    }

    private LauncherDiscoveryRequest toDiscoveryRequest(TestFile testFile) {
        List<DiscoverySelector> selectors = toSelectors(testFile)
                .collect(Collectors.toList());
        LOG.info("Executing request with selectors {}", selectors);
        LauncherDiscoveryRequestBuilder builder = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectors);
        if (testFile.hasTestCasesFilter()) {
            String testCaseFilter = toTestCaseFilter(testFile);
            builder = builder.configurationParameter(ARCHUNIT_INCLUDES_PARAMETER_NAME, testCaseFilter);
            LOG.info("Executing request with test case filter {}", testCaseFilter);
        }
        return builder.build();
    }

    private String toTestCaseFilter(TestFile testFile) {
        return testFile.getTestCases().stream().map((testFile.getFixture().getSimpleName() + ".")::concat).collect(Collectors.joining(","));
    }

    private Stream<DiscoverySelector> toSelectors(TestFile testFile) {
        if (shouldApplyTestMethodNameSelector(testFile) && testFile.hasTestCasesFilter()) {
            return testFile.getTestCases().stream().map(testCase -> toSelector(testFile.getFixture(), testCase));
        }
        return Stream.of(toTestClassSelector(testFile));
    }

    private DiscoverySelector toTestClassSelector(TestFile testFile) {
        return selectClass(testFile.getFixture());
    }

    private boolean shouldApplyTestMethodNameSelector(TestFile testFile) {
        // skip for ArchUnit tests because archunit.junit.includeTestsMatching will be used instead
        return testFile.getFixture().getSimpleName().contains("Regular");
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
                testExecutionResult.getThrowable().ifPresent(throwable -> {
                    throw new RuntimeException(throwable);
                });
                return;
            }
            registerTestResult(testIdentifier, toResult(testExecutionResult.getStatus()));
        }

        private void registerTestResult(TestIdentifier testIdentifier, TestResult testResult) {
            Map.Entry<Class<?>, String> testCaseEntry = testIdentifier.getSource()
                    .map(source -> resolveTestCaseInfo(testIdentifier, source))
                    .orElseThrow(RuntimeException::new);
            report.ensureFileForFixture(testCaseEntry.getKey().getName()).addResult(testCaseEntry.getValue(), testResult);
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
