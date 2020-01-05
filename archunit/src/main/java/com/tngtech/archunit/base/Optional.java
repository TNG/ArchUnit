/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.base;

import java.util.Objects;
import java.util.Set;

import com.tngtech.archunit.PublicAPI;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;

@PublicAPI(usage = ACCESS)
public abstract class Optional<T> {
    private Optional() {
    }

    @PublicAPI(usage = ACCESS)
    public static <T> Optional<T> of(T object) {
        checkNotNull(object, "Object may not be null");
        return new Present<>(object);
    }

    @PublicAPI(usage = ACCESS)
    public static <T> Optional<T> fromNullable(T object) {
        return object == null ? new Absent<T>() : new Present<>(object);
    }

    @PublicAPI(usage = ACCESS)
    @SuppressWarnings("unchecked")
    public static <T> Optional<T> absent() {
        return Absent.getInstance();
    }

    @PublicAPI(usage = ACCESS)
    public abstract boolean isPresent();

    @PublicAPI(usage = ACCESS)
    public abstract T get();

    @PublicAPI(usage = ACCESS)
    public abstract T getOrThrow(RuntimeException e);

    @PublicAPI(usage = ACCESS)
    public abstract <U> Optional<U> transform(Function<? super T, U> function);

    @PublicAPI(usage = ACCESS)
    public abstract T orNull();

    @PublicAPI(usage = ACCESS)
    public abstract T or(T value);

    @PublicAPI(usage = ACCESS)
    public abstract Optional<T> or(Optional<T> value);

    @PublicAPI(usage = ACCESS)
    public abstract Set<T> asSet();

    private static class Absent<T> extends Optional<T> {
        private static final Absent<Object> INSTANCE = new Absent<>();

        @SuppressWarnings("unchecked")
        private static <T> Absent<T> getInstance() {
            return (Absent<T>) INSTANCE;
        }

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

        private Present(T object) {
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
            final Present<?> other = (Present<?>) obj;
            return Objects.equals(this.object, other.object);
        }
    }
}
