package com.tngtech.archunit.base;

import org.junit.Test;

import static com.tngtech.archunit.base.Predicates.alwaysFalse;
import static com.tngtech.archunit.base.Predicates.alwaysTrue;
import static org.assertj.core.api.Assertions.assertThat;

public class PredicatesTest {

    @Test
    public void alwaysTrue_works() {
        assertThat(alwaysTrue().test(new Object())).isTrue();
    }

    @Test
    public void alwaysFalse_works() {
        assertThat(alwaysFalse().test(new Object())).isFalse();
    }
}
