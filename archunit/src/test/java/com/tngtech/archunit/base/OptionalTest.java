package com.tngtech.archunit.base;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class OptionalTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void isPresent_works() {
        assertThat(Optional.of(4).isPresent()).isTrue();
        assertThat(Optional.absent().isPresent()).isFalse();
    }

    @Test
    public void get_works() {
        assertThat(Optional.of("test").get()).isEqualTo("test");

        thrown.expect(NullPointerException.class);
        Optional.absent().get();
    }

    @Test
    public void getOrThrow_works() {
        assertThat(Optional.of("test").getOrThrow(new IllegalStateException("Bummer"))).isEqualTo("test");

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("Bummer");
        Optional.absent().getOrThrow(new IllegalStateException("Bummer"));
    }

    @Test
    public void transform_works() {
        assertThat(Optional.of(5).transform(TO_STRING)).isEqualTo(Optional.of("5"));

        assertThat(Optional.absent().transform(TO_STRING)).isEqualTo(Optional.absent());
    }

    @Test
    public void orNull_works() {
        assertThat(Optional.of("test").orNull()).isEqualTo("test");
        assertThat(Optional.absent().orNull()).isNull();
    }

    @Test
    public void or_works() {
        assertThat(Optional.of("test").or("other")).isEqualTo("test");
        assertThat(Optional.absent().or("other")).isEqualTo("other");
        assertThat(Optional.of("test").or(Optional.of("other"))).isEqualTo(Optional.of("test"));
        assertThat(Optional.absent().or(Optional.of("other"))).isEqualTo(Optional.of("other"));
    }

    @Test
    public void equals_and_hashcode() {
        assertThat(Optional.of(5)).isEqualTo(Optional.of(5));
        assertThat(Optional.of(5).hashCode()).as("HashCode").isEqualTo(Optional.of(5).hashCode());
        assertThat(Optional.of(5)).isNotEqualTo(Optional.of(4));

        assertThat(Optional.absent()).isEqualTo(Optional.absent());
        assertThat(Optional.absent().hashCode()).as("HashCode").isEqualTo(Optional.absent().hashCode());

    }

    private static final Function<Object, String> TO_STRING = new Function<Object, String>() {
        @Override
        public String apply(Object input) {
            return "" + input;
        }
    };
}