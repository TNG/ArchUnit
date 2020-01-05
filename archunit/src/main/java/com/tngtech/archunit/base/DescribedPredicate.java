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

import com.tngtech.archunit.PublicAPI;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

/**
 * A predicate holding a description.
 *
 * @param <T> The type of objects the predicate applies to
 */
@PublicAPI(usage = INHERITANCE)
public abstract class DescribedPredicate<T> implements Predicate<T> {
    private final String description;

    public DescribedPredicate(String description, Object... params) {
        checkArgument(description != null, "Description must be set");
        this.description = String.format(description, params);
    }

    public String getDescription() {
        return description;
    }

    public DescribedPredicate<T> as(String description, Object... params) {
        return new AsPredicate<>(this, description, params);
    }

    public DescribedPredicate<T> and(final DescribedPredicate<? super T> other) {
        return new AndPredicate<>(this, other);
    }

    public DescribedPredicate<T> or(final DescribedPredicate<? super T> other) {
        return new OrPredicate<>(this, other);
    }

    public <F> DescribedPredicate<F> onResultOf(final Function<? super F, ? extends T> function) {
        return new OnResultOfPredicate<>(this, function);
    }

    /**
     * Workaround for the limitations of the Java type system {@code ->} Can't specify this contravariant type at the language level
     */
    @SuppressWarnings("unchecked") // DescribedPredicate is contravariant
    public <U extends T> DescribedPredicate<U> forSubType() {
        return (DescribedPredicate<U>) this;
    }

    @Override
    public String toString() {
        return getDescription();
    }

    @SuppressWarnings("unchecked")
    public static <T> DescribedPredicate<T> alwaysTrue() {
        return (DescribedPredicate<T>) ALWAYS_TRUE;
    }

    private static final DescribedPredicate<Object> ALWAYS_TRUE = new DescribedPredicate<Object>("always true") {
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
        return new EqualToPredicate<>(object);
    }

    public static <T extends Comparable<T>> DescribedPredicate<T> lessThan(final T value) {
        return new LessThanPredicate<>(value);
    }

    public static <T extends Comparable<T>> DescribedPredicate<T> greaterThan(final T value) {
        return new GreaterThanPredicate<>(value);
    }

    public static <T extends Comparable<T>> DescribedPredicate<T> lessThanOrEqualTo(final T value) {
        return new LessThanOrEqualToPredicate<>(value);
    }

    public static <T extends Comparable<T>> DescribedPredicate<T> greaterThanOrEqualTo(final T value) {
        return new GreaterThanOrEqualToPredicate<>(value);
    }

    public static <T> DescribedPredicate<T> describe(String description, Predicate<? super T> predicate) {
        return new DescribePredicate<>(description, predicate).forSubType();
    }

    /**
     * @deprecated Decided to consistently never use contractions -&gt; use {@link #doesNot(DescribedPredicate)}
     */
    @Deprecated
    public static <T> DescribedPredicate<T> doesnt(final DescribedPredicate<? super T> predicate) {
        return not(predicate).as("doesn't %s", predicate.getDescription()).forSubType();
    }

    /**
     * @deprecated Decided to consistently never use contractions -&gt; use {@link #doNot(DescribedPredicate)}
     */
    @Deprecated
    public static <T> DescribedPredicate<T> dont(final DescribedPredicate<? super T> predicate) {
        return not(predicate).as("don't %s", predicate.getDescription()).forSubType();
    }

    public static <T> DescribedPredicate<T> doesNot(final DescribedPredicate<? super T> predicate) {
        return not(predicate).as("does not %s", predicate.getDescription()).forSubType();
    }

    public static <T> DescribedPredicate<T> doNot(final DescribedPredicate<? super T> predicate) {
        return not(predicate).as("do not %s", predicate.getDescription()).forSubType();
    }

    public static <T> DescribedPredicate<T> not(final DescribedPredicate<? super T> predicate) {
        return new NotPredicate<>(predicate);
    }

    public static <T> DescribedPredicate<Iterable<T>> anyElementThat(final DescribedPredicate<? super T> predicate) {
        return new AnyElementPredicate<>(predicate);
    }

    public static <T> DescribedPredicate<Iterable<T>> allElements(final DescribedPredicate<? super T> predicate) {
        return new AllElementsPredicate<>(predicate);
    }

    private static class AsPredicate<T> extends DescribedPredicate<T> {
        private final DescribedPredicate<T> current;

        AsPredicate(DescribedPredicate<T> current, String description, Object... params) {
            super(description, params);
            this.current = current;
        }

        @Override
        public boolean apply(T input) {
            return current.apply(input);
        }
    }

    private static class AndPredicate<T> extends DescribedPredicate<T> {
        private final DescribedPredicate<T> current;
        private final DescribedPredicate<? super T> other;

        AndPredicate(DescribedPredicate<T> current, DescribedPredicate<? super T> other) {
            super(current.getDescription() + " and " + other.getDescription());
            this.current = checkNotNull(current);
            this.other = checkNotNull(other);
        }

        @Override
        public boolean apply(T input) {
            return current.apply(input) && other.apply(input);
        }
    }

    private static class OrPredicate<T> extends DescribedPredicate<T> {
        private final DescribedPredicate<T> current;
        private final DescribedPredicate<? super T> other;

        OrPredicate(DescribedPredicate<T> current, DescribedPredicate<? super T> other) {
            super(current.getDescription() + " or " + other.getDescription());
            this.current = checkNotNull(current);
            this.other = checkNotNull(other);
        }

        @Override
        public boolean apply(T input) {
            return current.apply(input) || other.apply(input);
        }
    }

    private static class OnResultOfPredicate<F, T> extends DescribedPredicate<F> {
        private final DescribedPredicate<T> current;
        private final Function<? super F, ? extends T> function;

        OnResultOfPredicate(DescribedPredicate<T> current, Function<? super F, ? extends T> function) {
            super(current.getDescription());
            this.current = checkNotNull(current);
            this.function = checkNotNull(function);
        }

        @Override
        public boolean apply(F input) {
            return current.apply(function.apply(input));
        }
    }

    private static class NotPredicate<T> extends DescribedPredicate<T> {
        private final DescribedPredicate<T> predicate;

        NotPredicate(DescribedPredicate<? super T> predicate) {
            super("not " + predicate.getDescription());
            this.predicate = checkNotNull(predicate).forSubType();
        }

        @Override
        public boolean apply(T input) {
            return !predicate.apply(input);
        }
    }

    private static class EqualToPredicate<T> extends DescribedPredicate<T> {
        private final T value;

        EqualToPredicate(T value) {
            super("equal to '%s'", value);
            this.value = checkNotNull(value);
        }

        @Override
        public boolean apply(T input) {
            return value.equals(input);
        }
    }

    private static class LessThanPredicate<T extends Comparable<T>> extends DescribedPredicate<T> {
        private final T value;

        LessThanPredicate(T value) {
            super("less than '%s'", value);
            this.value = checkNotNull(value);
        }

        @Override
        public boolean apply(T input) {
            return input.compareTo(value) < 0;
        }
    }

    private static class GreaterThanPredicate<T extends Comparable<T>> extends DescribedPredicate<T> {
        private final T value;

        GreaterThanPredicate(T value) {
            super("greater than '%s'", value);
            this.value = checkNotNull(value);
        }

        @Override
        public boolean apply(T input) {
            return input.compareTo(value) > 0;
        }
    }

    private static class LessThanOrEqualToPredicate<T extends Comparable<T>> extends DescribedPredicate<T> {
        private final T value;

        LessThanOrEqualToPredicate(T value) {
            super("less than or equal to '%s'", value);
            this.value = checkNotNull(value);
        }

        @Override
        public boolean apply(T input) {
            return input.compareTo(value) <= 0;
        }
    }

    private static class GreaterThanOrEqualToPredicate<T extends Comparable<T>> extends DescribedPredicate<T> {
        private final T value;

        GreaterThanOrEqualToPredicate(T value) {
            super("greater than or equal to '%s'", value);
            this.value = checkNotNull(value);
        }

        @Override
        public boolean apply(T input) {
            return input.compareTo(value) >= 0;
        }
    }

    private static class DescribePredicate<T> extends DescribedPredicate<T> {
        private final Predicate<T> delegate;

        DescribePredicate(String description, Predicate<T> predicate) {
            super(description);
            this.delegate = checkNotNull(predicate);
        }

        @Override
        public boolean apply(T input) {
            return delegate.apply(input);
        }
    }

    private static class AnyElementPredicate<T> extends DescribedPredicate<Iterable<T>> {
        private final DescribedPredicate<T> predicate;

        AnyElementPredicate(DescribedPredicate<? super T> predicate) {
            super("any element that " + predicate.getDescription());
            this.predicate = predicate.forSubType();
        }

        @Override
        public boolean apply(Iterable<T> iterable) {
            for (T javaClass : iterable) {
                if (predicate.apply(javaClass)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class AllElementsPredicate<T> extends DescribedPredicate<Iterable<T>> {
        private final DescribedPredicate<T> predicate;

        AllElementsPredicate(DescribedPredicate<? super T> predicate) {
            super("all elements " + predicate.getDescription());
            this.predicate = predicate.forSubType();
        }

        @Override
        public boolean apply(Iterable<T> iterable) {
            for (T javaClass : iterable) {
                if (!predicate.apply(javaClass)) {
                    return false;
                }
            }
            return true;
        }
    }
}
