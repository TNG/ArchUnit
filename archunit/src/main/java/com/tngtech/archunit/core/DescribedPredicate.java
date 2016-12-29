package com.tngtech.archunit.core;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A predicate holding a description.
 *
 * @param <T> The type of objects the predicate applies to
 */
public abstract class DescribedPredicate<T> {
    private String description;

    public abstract boolean apply(T input);

    public DescribedPredicate(String description, Object... params) {
        checkArgument(description != null, "Description must be set");
        this.description = String.format(description, params);
    }

    public String getDescription() {
        return description;
    }

    public DescribedPredicate<T> as(String description, Object... params) {
        return new DescribedPredicate<T>(description, params) {
            @Override
            public boolean apply(T input) {
                return DescribedPredicate.this.apply(input);
            }
        };
    }

    public DescribedPredicate<T> and(final DescribedPredicate<? super T> other) {
        return new DescribedPredicate<T>(description + " and " + other.getDescription()) {
            @Override
            public boolean apply(T input) {
                return DescribedPredicate.this.apply(input) && other.apply(input);
            }
        };
    }

    public DescribedPredicate<T> or(final DescribedPredicate<? super T> other) {
        return new DescribedPredicate<T>(description + " or " + other.getDescription()) {
            @Override
            public boolean apply(T input) {
                return DescribedPredicate.this.apply(input) || other.apply(input);
            }
        };
    }

    public <F> DescribedPredicate<F> onResultOf(final Function<? super F, ? extends T> function) {
        checkNotNull(function);
        return new DescribedPredicate<F>(description) {
            @Override
            public boolean apply(F input) {
                return DescribedPredicate.this.apply(function.apply(input));
            }
        };
    }

    @SuppressWarnings("unchecked")
    public static <T> DescribedPredicate<T> alwaysTrue() {
        return (DescribedPredicate<T>) ALWAYS_TRUE;
    }

    private static DescribedPredicate<Object> ALWAYS_TRUE = new DescribedPredicate<Object>("always true") {
        @Override
        public boolean apply(Object input) {
            return true;
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> DescribedPredicate<T> alwaysFalse() {
        return (DescribedPredicate<T>) ALWAYS_FALSE;
    }

    private static final DescribedPredicate<Object> ALWAYS_FALSE = new DescribedPredicate<Object>("always false") {
        @Override
        public boolean apply(Object input) {
            return false;
        }
    };

    public static <T> DescribedPredicate<T> equalTo(final T object) {
        checkNotNull(object);
        return new DescribedPredicate<T>("equal to '%s'", object) {
            @Override
            public boolean apply(T input) {
                return object.equals(input);
            }
        };
    }

    public static <T> DescribedPredicate<T> not(final DescribedPredicate<T> predicate) {
        checkNotNull(predicate);
        return new DescribedPredicate<T>("not " + predicate.getDescription()) {
            @Override
            public boolean apply(T input) {
                return !predicate.apply(input);
            }
        };
    }

    @SuppressWarnings("unchecked") // DescribedPredicate is contra variant
    public <U extends T> DescribedPredicate<U> forSubType() {
        return (DescribedPredicate<U>) this;
    }
}
