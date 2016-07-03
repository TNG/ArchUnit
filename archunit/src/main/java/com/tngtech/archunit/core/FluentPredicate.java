package com.tngtech.archunit.core;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class FluentPredicate<T> {
    public abstract boolean apply(T input);

    public FluentPredicate<T> and(final FluentPredicate<T> other) {
        return new FluentPredicate<T>() {
            @Override
            public boolean apply(T input) {
                return FluentPredicate.this.apply(input) && other.apply(input);
            }
        };
    }

    public FluentPredicate<T> or(final FluentPredicate<T> other) {
        return new FluentPredicate<T>() {
            @Override
            public boolean apply(T input) {
                return FluentPredicate.this.apply(input) || other.apply(input);
            }
        };
    }

    public <F> FluentPredicate<F> onResultOf(final Function<F, ? extends T> function) {
        checkNotNull(function);
        return new FluentPredicate<F>() {
            @Override
            public boolean apply(F input) {
                return FluentPredicate.this.apply(function.apply(input));
            }
        };
    }

    public static <T> FluentPredicate<T> alwaysTrue() {
        return new FluentPredicate<T>() {
            @Override
            public boolean apply(T input) {
                return true;
            }
        };
    }

    public static <T> FluentPredicate<T> alwaysFalse() {
        return new FluentPredicate<T>() {
            @Override
            public boolean apply(T input) {
                return false;
            }
        };
    }

    public static <T> FluentPredicate<T> equalTo(final T object) {
        checkNotNull(object);
        return new FluentPredicate<T>() {
            @Override
            public boolean apply(T input) {
                return object.equals(input);
            }
        };
    }

    public static <T> FluentPredicate<T> not(final FluentPredicate<T> predicate) {
        checkNotNull(predicate);
        return new FluentPredicate<T>() {
            @Override
            public boolean apply(T input) {
                return !predicate.apply(input);
            }
        };
    }

}
