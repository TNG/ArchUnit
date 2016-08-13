package com.tngtech.archunit.core;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

public abstract class Optional<T> {
    private static final Absent<Object> ABSENT = new Absent<>();

    private Optional() {
    }

    public static <T> Optional<T> of(T object) {
        checkNotNull(object, "Object may not be null");
        return new Present<>(object);
    }

    public static <T> Optional<T> fromNullable(T object) {
        return object == null ? new Absent<T>() : new Present<>(object);
    }

    @SuppressWarnings("unchecked")
    public static <T> Optional<T> absent() {
        return (Optional<T>) ABSENT;
    }

    public abstract boolean isPresent();

    public abstract T get();

    public abstract T getOrThrow(RuntimeException e);

    public abstract <U> Optional<U> transform(Function<? super T, U> function);

    public abstract T orNull();

    public abstract T or(T value);

    public abstract Optional<T> or(Optional<T> value);

    public abstract Set<T> asSet();

    private static class Absent<T> extends Optional<T> {
        @Override
        public boolean isPresent() {
            return false;
        }

        @Override
        public T get() {
            throw new NullPointerException("Object is absent");
        }

        @Override
        public T orNull() {
            return null;
        }

        @Override
        public T or(T value) {
            return value;
        }

        @Override
        public Optional<T> or(Optional<T> value) {
            return value;
        }

        @Override
        public Set<T> asSet() {
            return emptySet();
        }

        @Override
        public T getOrThrow(RuntimeException e) {
            throw e;
        }

        @Override
        public <U> Optional<U> transform(Function<? super T, U> function) {
            return absent();
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Absent;
        }
    }

    private static class Present<T> extends Optional<T> {
        private final T object;

        public Present(T object) {
            this.object = object;
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public T get() {
            return object;
        }

        @Override
        public T orNull() {
            return object;
        }

        @Override
        public T or(T value) {
            return object;
        }

        @Override
        public Optional<T> or(Optional<T> value) {
            return this;
        }

        @Override
        public Set<T> asSet() {
            return singleton(object);
        }

        @Override
        public T getOrThrow(RuntimeException e) {
            return object;
        }

        @Override
        public <U> Optional<U> transform(Function<? super T, U> function) {
            return Optional.of(function.apply(object));
        }

        @Override
        public int hashCode() {
            return Objects.hash(object);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final Present other = (Present) obj;
            return Objects.equals(this.object, other.object);
        }
    }
}
