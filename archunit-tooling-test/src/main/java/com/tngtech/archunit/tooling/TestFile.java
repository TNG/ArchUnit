package com.tngtech.archunit.tooling;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class TestFile {
    private final Class<?> fixture;
    private final Set<String> testCases;

    public TestFile(Class<?> fixture, Set<String> testCases) {
        this.fixture = fixture;
        this.testCases = testCases;
    }

    public TestFile(Class<?> fixture, String testCase, String... more) {
        this(fixture, toSet(testCase, more));
    }

    private static Set<String> toSet(String testCase, String[] more) {
        Set<String> result = new HashSet<>();
        result.add(testCase);
        result.addAll(Arrays.asList(more));
        return result;
    }

    public TestFile(Class<?> fixture) {
        this(fixture, null);
    }

    public Class<?> getFixture() {
        return fixture;
    }

    public Set<String> getTestCases() {
        return testCases;
    }

    public boolean hasTestCasesFilter() {
        return Objects.nonNull(testCases);
    }

    public TestingFramework getTestingFramework() {
        return TestingFramework.JUNIT5;
    }

    public enum TestingFramework {
        JUNIT4,
        JUNIT5
    }

    @Override
    public String toString() {
        if (hasTestCasesFilter()) {
            return testCases + " in " + fixture.getSimpleName();
        }
        return "All tests in " + fixture.getSimpleName();
    }
}
