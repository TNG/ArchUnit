package com.tngtech.archunit.tooling;

import java.lang.reflect.Field;
import java.util.Optional;

import com.tngtech.archunit.junit.ArchTests;

class TestResults {

    private final TestReport report;
    private final TestFile testFile;

    private TestResults(TestReport report, TestFile testFile) {
        this.report = report;
        this.testFile = testFile;
    }

    static TestResults of(TestReport report, TestFile testFile) {
        return new TestResults(report, testFile);
    }

    boolean hasInitializationError() {
        return report.getFiles().stream().anyMatch(ExecutedTestFile::hasInitializationError);
    }

    public ExecutedTestFile.TestResult getResult(String testCase) {
        return getResult(testFile.getFixture(), testCase);
    }

    private ExecutedTestFile.TestResult getResult(Class<?> fixture, String testCase) {
        return maybeGetResult(fixture, testCase)
                .orElseThrow(() -> new RuntimeException("No result found for " + testCase));
    }

    boolean hasResult(String testCase) {
        return maybeGetResult(testFile.getFixture(), testCase)
                .isPresent();
    }

    int resultCount() {
        return report.getFiles().stream()
                .mapToInt(ExecutedTestFile::size)
                .sum();
    }

    private Optional<ExecutedTestFile.TestResult> maybeGetResult(Class<?> fixture, String testCase) {
        if (testFile.isSuite()) {
            return getSuiteResult(fixture, testCase);
        }
        return getTestResult(fixture, testCase);
    }

    private Optional<ExecutedTestFile.TestResult> getTestResult(Class<?> fixture, String testCase) {
        return report.getFile(fixture.getName())
                .flatMap(file -> file.getResult(testCase));
    }

    private Optional<ExecutedTestFile.TestResult> getSuiteResult(Class<?> fixture, String testCase) {
        try {
            Field suiteField = fixture.getDeclaredField(testCase);
            suiteField.setAccessible(true);
            ArchTests archTests = (ArchTests) suiteField.get(fixture);
            Optional<ExecutedTestFile.TestResult> rule = getTestResult(archTests.getDefinitionLocation(), "rule");
            return rule.isPresent()
                    ? rule
                    // this is how Gradle reports ArchUnit rule AssertionErrors for ArchUnit suites in JUnit 4 tests
                    : getTestResult(archTests.getDefinitionLocation(), "failed to execute tests");
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Invalid rule definition class");
        }
    }
}
