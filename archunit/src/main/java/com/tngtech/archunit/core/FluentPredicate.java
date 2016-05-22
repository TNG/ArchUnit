package com.tngtech.archunit.core;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public abstract class FluentPredicate<T> implements Predicate<T> {
    public FluentPredicate<T> and(Predicate<T> other) {
        return FluentPredicate.of(Predicates.and(this, other));
    }

    public FluentPredicate<T> or(Predicate<T> other) {
        return FluentPredicate.of(Predicates.or(this, other));
    }

    public static <T> FluentPredicate<T> of(Predicate<T> predicate) {
        return new DelegatingPredicate<>(predicate);
    }

    private static class DelegatingPredicate<T> extends FluentPredicate<T> {
        private final Predicate<T> delegate;

        private DelegatingPredicate(Predicate<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean apply(T input) {
            return delegate.apply(input);
        }
    }
}
