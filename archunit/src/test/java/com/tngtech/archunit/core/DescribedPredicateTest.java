package com.tngtech.archunit.core;

import org.junit.Test;

import static com.tngtech.archunit.core.DescribedPredicate.alwaysFalse;
import static com.tngtech.archunit.core.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.DescribedPredicate.not;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class DescribedPredicateTest {

    @Test
    public void alwaysTrue_works() {
        assertThat(alwaysTrue().apply(new Object())).isTrue();
        assertThat(alwaysTrue().getDescription()).contains("always true");
    }

    @Test
    public void alwaysFalse_works() {
        assertThat(alwaysFalse().apply(new Object())).isFalse();
        assertThat(alwaysFalse().getDescription()).contains("always false");
    }

    @Test
    public void and_works() {
        assertThat(alwaysFalse().and(alwaysFalse()).apply(new Object())).isFalse();
        assertThat(alwaysFalse().and(alwaysTrue()).apply(new Object())).isFalse();
        assertThat(alwaysTrue().and(alwaysFalse()).apply(new Object())).isFalse();
        assertThat(alwaysTrue().and(alwaysTrue()).apply(new Object())).isTrue();
    }

    @Test
    public void or_works() {
        assertThat(alwaysFalse().or(alwaysFalse()).apply(new Object())).isFalse();
        assertThat(alwaysFalse().or(alwaysTrue()).apply(new Object())).isTrue();
        assertThat(alwaysTrue().or(alwaysFalse()).apply(new Object())).isTrue();
        assertThat(alwaysTrue().or(alwaysTrue()).apply(new Object())).isTrue();
    }

    @Test
    public void equalTo_works() {
        assertThat(equalTo(5).apply(4)).isFalse();
        assertThat(equalTo(5).getDescription()).contains("equal to '5'");
        assertThat(equalTo(5).apply(5)).isTrue();
        assertThat(equalTo(5).apply(6)).isFalse();

        Object object = new Object();
        assertThat(equalTo(object).apply(object)).isTrue();
    }

    @Test
    public void not_works() {
        assertThat(not(equalTo(5)).apply(4)).isTrue();
        assertThat(not(equalTo(5)).getDescription()).contains("not equal to '5'");
        assertThat(not(equalTo(5)).apply(5)).isFalse();
        assertThat(not(equalTo(5)).apply(6)).isTrue();

        Object object = new Object();
        assertThat(not(equalTo(object)).apply(object)).isFalse();
    }

    @Test
    public void onResultOf_works() {
        assertThat(equalTo(5).onResultOf(constant(4)).apply(new Object())).isFalse();
        assertThat(equalTo(5).onResultOf(constant(5)).apply(new Object())).isTrue();
        assertThat(equalTo(5).onResultOf(constant(6)).apply(new Object())).isFalse();
    }

    private Function<Object, Integer> constant(final int integer) {
        return new Function<Object, Integer>() {
            @Override
            public Integer apply(Object input) {
                return integer;
            }
        };
    }
}