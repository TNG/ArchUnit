package com.tngtech.archunit.tooling;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ExecutedTestFile {
    private final Class<?> fixture;
    private final Map<String, TestResult> results = new ConcurrentHashMap<>();

    public ExecutedTestFile(Class<?> fixture) {
        this.fixture = fixture;
    }

    public Class<?> getFixture() {
        return fixture;
    }

    public Map<String, TestResult> getResults() {
        return Collections.unmodifiableMap(results);
    }

    public TestResult getResult(String testCase) {
        return results.get(testCase);
    }

    public void addResult(String testCase, TestResult result) {
        results.put(testCase, result);
    }

    public enum TestResult {
        SUCCESS,
        FAILURE,
        ERROR,
        SKIPPED
    }
}
