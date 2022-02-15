package com.tngtech.archunit.base;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OptionalTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void isPresent_works() {
        assertThat(Optional.of(4).isPresent()).isTrue();
        assertThat(Optional.empty().isPresent()).isFalse();
        assertThat(Optional.absent().isPresent()).isFalse();
    }

    @Test
    public void get_works() {
        assertThat(Optional.of("test").get()).isEqualTo("test");

        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                Optional.empty().get();
            }
        }).isInstanceOf(NullPointerException.class);

        assertThatThrownBy(new ThrowingCallable() {
            @Override
            public void call() {
                Optional.absent().get();
            }
        }).isInstanceOf(NullPointerException.class);
    }

    @Test
    public void getOrThrow_works() {
        assertThat(Optional.of("test").getOrThrow(new Supplier<IllegalStateException>() {
            @Override
            public IllegalStateException get() {
                return new IllegalStateException("SupplierBummer");
            }
        })).isEqualTo("test");

        thrown.expect(IllegalStateException.class);
        thrown.expectMessage("SupplierBummer");
        Optional.empty().orElseThrow(new Supplier<IllegalStateException>() {
            @Override
            public IllegalStateException get() {
                return new IllegalStateException("SupplierBummer");
            }
        });
    }

    @Test
    public void map_works() {
        assertThat(Optional.of(5).transform(TO_STRING)).isEqualTo(Optional.of("5"));
        assertThat(Optional.of(5).map(TO_STRING)).isEqualTo(Optional.of("5"));

        assertThat(Optional.empty().transform(TO_STRING)).isEqualTo(Optional.empty());
        assertThat(Optional.empty().map(TO_STRING)).isEqualTo(Optional.empty());
    }

    @Test
    public void orNull_works() {
        assertThat(Optional.of("test").orNull()).isEqualTo("test");
        assertThat(Optional.absent().orNull()).isNull();
    }

    @Test
    public void orElse_works() {
        assertThat(Optional.of("test").orElse("other")).isEqualTo("test");
        assertThat(Optional.empty().orElse("other")).isEqualTo("other");
        assertThat(Optional.empty().orElse(null)).isNull();

        assertThat(Optional.of("test").or(Optional.of("other"))).isEqualTo(Optional.of("test"));
        assertThat(Optional.empty().or(Optional.of("other"))).isEqualTo(Optional.of("other"));
    }

    @Test
    public void orElseGet_works() {
        assertThat(Optional.of("test").orElseGet(new Supplier<String>() {
            @Override
            public String get() {
                return "other";
            }
        })).isEqualTo("test");
        assertThat(Optional.empty().orElseGet(new Supplier<String>() {
            @Override
            public String get() {
                return "other";
            }
        })).isEqualTo("other");
        assertThat(Optional.empty().orElseGet(new Supplier<String>() {
            @Override
            public String get() {
                return null;
            }
        })).isNull();
    }

    @Test
    public void equals_and_hashcode() {
        assertThat(Optional.of(5)).isEqualTo(Optional.of(5));
        assertThat(Optional.of(5).hashCode()).as("HashCode").isEqualTo(Optional.of(5).hashCode());
        assertThat(Optional.of(5)).isNotEqualTo(Optional.of(4));

        assertThat(Optional.empty()).isEqualTo(Optional.empty());
        assertThat(Optional.empty().hashCode()).as("HashCode").isEqualTo(Optional.empty().hashCode());

    }

    private static final Function<Object, String> TO_STRING = new Function<Object, String>() {
        @Override
        public String apply(Object input) {
            return "" + input;
        }
    };
}
