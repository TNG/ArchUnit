package com.tngtech.archunit.base;

import org.junit.Test;

import static com.tngtech.archunit.base.Predicate.Defaults.alwaysFalse;
import static com.tngtech.archunit.base.Predicate.Defaults.alwaysTrue;
import static org.assertj.core.api.Assertions.assertThat;

public class PredicateTest {

    @Test
    public void alwaysTrue_works() {
        assertThat(alwaysTrue().apply(new Object())).isTrue();
    }

    @Test
    public void alwaysFalse_works() {
        assertThat(alwaysFalse().apply(new Object())).isFalse();
    }
}