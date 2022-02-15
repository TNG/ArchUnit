/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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

    /**
     * @deprecated use {@link #ofNullable(Object)} instead
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    public static <T> Optional<T> fromNullable(T object) {
        return ofNullable(object);
    }

    @PublicAPI(usage = ACCESS)
    public static <T> Optional<T> ofNullable(T object) {
        return object == null ? Optional.<T>empty() : new Present<>(object);
    }

    /**
     * @deprecated use {@link #empty()} instead
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    public static <T> Optional<T> absent() {
        return empty();
    }

    @PublicAPI(usage = ACCESS)
    public static <T> Optional<T> empty() {
        return Empty.getInstance();
    }

    @PublicAPI(usage = ACCESS)
    public abstract boolean isPresent();

    @PublicAPI(usage = ACCESS)
    public abstract T get();

    /**
     * @deprecated Use {@link #orElseThrow(Supplier)} instead
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    public T getOrThrow(Supplier<? extends RuntimeException> exceptionSupplier) {
        return orElseThrow(exceptionSupplier);
    }

    @PublicAPI(usage = ACCESS)
    public abstract <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X;

    /**
     * @deprecated Use {@link #map(Function)} instead.
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    public <U> Optional<U> transform(Function<? super T, U> function) {
        return map(function);
    }

    @PublicAPI(usage = ACCESS)
    public abstract <U> Optional<U> map(Function<? super T, ? extends U> mapper);

    /**
     * @deprecated Use {@link #orElse(Object) orElse(null)} instead.
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    public T orNull() {
        return orElse(null);
    }

    /**
     * @deprecated Use {@link #orElse(Object)} instead.
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    public T or(T value) {
        return orElse(value);
    }

    @PublicAPI(usage = ACCESS)
    public abstract T orElse(T other);

    @PublicAPI(usage = ACCESS)
    public abstract T orElseGet(Supplier<? extends T> supplier);

    /**
     * @deprecated This method will be removed in the future, use {@link #orElseGet(Supplier)} instead.
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    public abstract Optional<T> or(Optional<? extends T> value);

    @PublicAPI(usage = ACCESS)
    public abstract Set<T> asSet();

    private static class Empty<T> extends Optional<T> {
        private static final Empty<Object> INSTANCE = new Empty<>();

        @SuppressWarnings("unchecked")
        private static <T> Empty<T> getInstance() {
            return (Empty<T>) INSTANCE;
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
        public T orElse(T value) {
            return value;
        }

        @Override
        public T orElseGet(Supplier<? extends T> supplier) {
            return supplier.get();
        }

        @Override
        @SuppressWarnings("unchecked") // cast is safe because Optional is covariant
        public Optional<T> or(Optional<? extends T> value) {
            return (Optional<T>) value;
        }

        @Override
        public Set<T> asSet() {
            return emptySet();
        }

        @Override
        public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
            throw exceptionSupplier.get();
        }

        @Override
        public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
            return empty();
        }

        @Override
        public int hashCode() {
            return 1;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Optional.Empty;
        }

        @Override
        public String toString() {
            return Optional.class.getSimpleName() + ".empty()";
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
        public T orElse(T value) {
            return object;
        }

        @Override
        public T orElseGet(Supplier<? extends T> supplier) {
            return object;
        }

        @Override
        public Optional<T> or(Optional<? extends T> value) {
            return this;
        }

        @Override
        public Set<T> asSet() {
            return singleton(object);
        }

        @Override
        public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) {
            return object;
        }

        @Override
        public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
            return Optional.of(mapper.apply(object));
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

        @Override
        public String toString() {
            return Optional.class.getSimpleName() + ".of(" + object + ")";
        }
    }
}
