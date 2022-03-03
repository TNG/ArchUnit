package com.tngtech.archunit.tooling;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

public class TestReport {

    private final Set<ExecutedTestFile> files = new HashSet<>();

    public Set<ExecutedTestFile> getFiles() {
        return Collections.unmodifiableSet(files);
    }

    public void addFile(ExecutedTestFile file) {
        files.add(file);
    }

    public Optional<ExecutedTestFile> getFile(Class<?> fixture) {
        return files.stream()
                .filter(file -> file.getFixture().equals(fixture))
                .findFirst();
    }

    public synchronized ExecutedTestFile ensureFileForFixture(Class<?> fixture) {
        return getFile(fixture)
                .orElseGet(() -> newTestFile(fixture));
    }

    private ExecutedTestFile newTestFile(Class<?> fixture) {
        ExecutedTestFile result = new ExecutedTestFile(fixture);
        files.add(result);
        return result;
    }
}
