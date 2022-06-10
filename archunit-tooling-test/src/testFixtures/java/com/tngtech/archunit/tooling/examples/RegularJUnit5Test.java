package com.tngtech.archunit.tooling.examples;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;

import static org.junit.jupiter.api.Assertions.fail;

public class RegularJUnit5Test {

    @Test
    void shouldReportSuccess() {

    }

    @Test
    void shouldReportFailure() {
        fail();
    }

    @Test
    void shouldReportError() {
        throw new RuntimeException();
    }

    @Test
    @Disabled
    void shouldBeSkipped() {

    }

    @Test
    @DisabledIfEnvironmentVariable(named = "SKIP_BY_ENV_VARIABLE", matches = "true")
    void shouldBeSkippedConditionally() {

    }
}
