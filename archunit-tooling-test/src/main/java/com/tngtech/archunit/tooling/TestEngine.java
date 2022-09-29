package com.tngtech.archunit.tooling;

public interface TestEngine {

    TestReport execute(TestFile testFiles) throws Exception;

    default boolean reportsErrors() {
        return false;
    }
}
