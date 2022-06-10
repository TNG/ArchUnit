package com.tngtech.archunit.tooling;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class ExecutedTestFile {
    private final String fixture;
    private final Map<String, TestResult> results = new ConcurrentHashMap<>();

    public ExecutedTestFile(String fixture) {
        this.fixture = fixture;
    }

    public String getFixture() {
        return fixture;
    }

    public Map<String, TestResult> getResults() {
        return Collections.unmodifiableMap(results);
    }

    public int size() {
        return results.size();
    }

    public Optional<TestResult> getResult(String testCase) {
        return Optional.ofNullable(results.get(testCase))
                .or(() -> findMatchingParameterizedTestName(testCase).map(results::get));
    }

    private Optional<String> findMatchingParameterizedTestName(String testCase) {
        Pattern parameterizedPattern = Pattern.compile(testCase + "\\(.*\\)");
        return results.keySet().stream()
                .filter(key -> parameterizedPattern.matcher(key).matches())
                .findAny();
    }

    public void addResult(String testCase, TestResult result) {
        results.put(testCase, result);
    }

    public boolean hasInitializationError() {
        return getResult("initializationError").isPresent();
    }

    public enum TestResult {
        SUCCESS,
        FAILURE,
        ERROR,
        SKIPPED
    }
}
