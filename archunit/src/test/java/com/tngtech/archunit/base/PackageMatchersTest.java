package com.tngtech.archunit.base;

import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class PackageMatchersTest {
    @Test
    public void matches_any_package() {
        assertThat(PackageMatchers.of("..match..", "..other.."))
                .accepts("foo.match.bar")
                .accepts("foo.other.bar")
                .accepts("foo.match.other.bar")
                .rejects("foo.bar")
                .rejects("matc.hother");
    }

    @Test
    public void description() {
        assertThat(PackageMatchers.of("..foo..", "..bar.."))
                .hasDescription("matches any of ['..foo..', '..bar..']");
    }
}