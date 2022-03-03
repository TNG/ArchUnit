package com.tngtech.archunit.tooling.examples;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assume.assumeThat;
import static org.junit.jupiter.api.Assertions.fail;

@RunWith(BlockJUnit4ClassRunner.class)
public class RegularJunit4Test {

    @Test
    public void shouldReportSuccess() {

    }

    @Test
    public void shouldReportFailure() {
        fail();
    }

    @Test
    public void shouldReportError() {
        throw new RuntimeException();
    }

    @Test
    @Ignore
    public void shouldBeSkipped() {

    }

    @Test
    public void shouldBeSkippedConditionally() {
        assumeThat(System.getenv("SKIP_BY_ENV_VARIABLE"), not(equalTo("true")));
    }
}
