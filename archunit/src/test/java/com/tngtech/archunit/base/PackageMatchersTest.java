package com.tngtech.archunit.base;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PackageMatchersTest {
    @Test
    public void matches_any_package() {
        PackageMatchers matchers = PackageMatchers.of("..match..", "..other..");

        assertThat(matchers.apply("foo.match.bar")).isTrue();
        assertThat(matchers.apply("foo.other.bar")).isTrue();
        assertThat(matchers.apply("foo.match.other.bar")).isTrue();
        assertThat(matchers.apply("foo.bar")).isFalse();
        assertThat(matchers.apply("matc.hother")).isFalse();
    }

    @Test
    public void description() {
        assertThat(PackageMatchers.of("..foo..", "..bar..").getDescription()).isEqualTo("matches any of ['..foo..', '..bar..']");
    }
}