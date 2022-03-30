package com.tngtech.archunit.tooling;

import java.util.Set;

public interface TestEngine {

    TestReport execute(Set<TestFile> testFiles) throws Exception;

    default boolean reportsErrors() {
        return false;
    }
}
