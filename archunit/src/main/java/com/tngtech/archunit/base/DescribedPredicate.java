/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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

import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.tngtech.archunit.PublicAPI;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Iterables.isEmpty;
import static com.google.common.collect.Iterables.size;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;
import static java.util.stream.StreamSupport.stream;

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

    /**
     * @return A {@link DescribedPredicate} that will return {@code true} whenever this predicate would return {@code false}, and vice versa.
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public DescribedPredicate<T> negate() {
        return new NotPredicate<>(this);
    }

    /**
     * Overwrites the description of this {@link DescribedPredicate}. E.g.
     *
     * <pre><code>
     * classes().that(predicate.as("some customized description with '%s'", "parameter")).should().bePublic()
     * </code></pre>
     *
     * would then yield {@code classes that some customized description with 'parameter' should be public}.
     *
     * @param description The new description of this {@link DescribedPredicate}
     * @param params Optional arguments to fill into the description via {@link String#format(String, Object...)}
     * @return An {@link DescribedPredicate} with adjusted {@link #getDescription() description}.
     */
    public DescribedPredicate<T> as(String description, Object... params) {
        return new AsPredicate<>(this, description, params);
    }

    public DescribedPredicate<T> and(DescribedPredicate<? super T> other) {
        return new AndPredicate<>(this, other);
    }

    public DescribedPredicate<T> or(DescribedPredicate<? super T> other) {
        return new OrPredicate<>(this, other);
    }

    public <F> DescribedPredicate<F> onResultOf(Function<? super F, ? extends T> function) {
        return new OnResultOfPredicate<>(this, function);
    }

    /**
     * Convenience method to downcast the predicate. {@link DescribedPredicate DescribedPredicates} are contravariant by nature,
     * i.e. an {@code DescribedPredicate<T>} is an instance of {@code DescribedPredicate<V>}, if and only if {@code V} is an instance of {@code T}.
     * <br>
     * Take for example {@code Object > String}. Obviously a {@code DescribedPredicate<Object>} is also a {@code DescribedPredicate<String>}.
     * <br>
     * Unfortunately, the Java type system does not allow us to express this property of the type parameter of {@code DescribedPredicate}.
     * So to avoid forcing users to cast everywhere it is possible to use this method which also documents the intention and reasoning.
     *
     * @return A {@link DescribedPredicate} accepting a subtype of the predicate's actual type parameter {@code T}
     * @param <U> A subtype of the {@link DescribedPredicate DescribedPredicate's} type parameter {@code T}
     */
    @SuppressWarnings("unchecked") // DescribedPredicate is contravariant
    public final <U extends T> DescribedPredicate<U> forSubtype() {
        return (DescribedPredicate<U>) this;
    }

    @Override
    public String toString() {
        return getDescription();
    }

    @PublicAPI(usage = ACCESS)
    @SuppressWarnings("unchecked")
    public static <T> DescribedPredicate<T> alwaysTrue() {
        return (DescribedPredicate<T>) ALWAYS_TRUE;
    }

    private static final DescribedPredicate<Object> ALWAYS_TRUE = new DescribedPredicate<Object>("always true") {
        @Override
        public boolean test(Object input) {
            return true;
        }
    };

    @PublicAPI(usage = ACCESS)
    @SuppressWarnings("unchecked")
    public static <T> DescribedPredicate<T> alwaysFalse() {
        return (DescribedPredicate<T>) ALWAYS_FALSE;
    }

    private static final DescribedPredicate<Object> ALWAYS_FALSE = new DescribedPredicate<Object>("always false") {
        @Override
        public boolean test(Object input) {
            return false;
        }
    };

    @PublicAPI(usage = ACCESS)
    public static <T> DescribedPredicate<T> equalTo(T object) {
        return new EqualToPredicate<>(object);
    }

    @PublicAPI(usage = ACCESS)
    public static <T extends Comparable<T>> DescribedPredicate<T> lessThan(T value) {
        return new LessThanPredicate<>(value);
    }

    @PublicAPI(usage = ACCESS)
    public static <T extends Comparable<T>> DescribedPredicate<T> greaterThan(T value) {
        return new GreaterThanPredicate<>(value);
    }

    @PublicAPI(usage = ACCESS)
    public static <T extends Comparable<T>> DescribedPredicate<T> lessThanOrEqualTo(T value) {
        return new LessThanOrEqualToPredicate<>(value);
    }

    @PublicAPI(usage = ACCESS)
    public static <T extends Comparable<T>> DescribedPredicate<T> greaterThanOrEqualTo(T value) {
        return new GreaterThanOrEqualToPredicate<>(value);
    }

    @PublicAPI(usage = ACCESS)
    public static <T> DescribedPredicate<T> describe(String description, Predicate<? super T> predicate) {
        return new DescribePredicate<>(description, predicate).forSubtype();
    }

    /**
     * Same as {@link #not(DescribedPredicate)} but with a different description
     */
    @PublicAPI(usage = ACCESS)
    public static <T> DescribedPredicate<T> doesNot(DescribedPredicate<? super T> predicate) {
        return not(predicate).as("does not %s", predicate.getDescription()).forSubtype();
    }

    /**
     * Same as {@link #not(DescribedPredicate)} but with a different description
     */
    @PublicAPI(usage = ACCESS)
    public static <T> DescribedPredicate<T> doNot(DescribedPredicate<? super T> predicate) {
        return not(predicate).as("do not %s", predicate.getDescription()).forSubtype();
    }

    /**
     * @param predicate Any {@link DescribedPredicate}
     * @return A predicate that will return {@code true} whenever the original predicate would return {@code false}, and vice versa.
     * @param <T> The type of object the {@link DescribedPredicate predicate} applies to
     */
    @PublicAPI(usage = ACCESS)
    public static <T> DescribedPredicate<T> not(DescribedPredicate<? super T> predicate) {
        return new NotPredicate<>(predicate);
    }

    /**
     * @see #and(Iterable)
     */
    @SafeVarargs
    @PublicAPI(usage = ACCESS)
    @SuppressWarnings("RedundantTypeArguments") // Unfortunately not really redundant with JDK 8 :-(
    public static <T> DescribedPredicate<T> and(DescribedPredicate<? super T>... predicates) {
        return and(ImmutableList.<DescribedPredicate<? super T>>copyOf(predicates));
    }

    /**
     * @param predicates Any number of {@link DescribedPredicate predicates} to join together via *AND*
     * @return A {@link DescribedPredicate predicate} that returns {@code true}, if all the supplied
     *         predicates return {@code true}. Otherwise, it returns {@code false}.
     *         If an empty {@link Iterable} is passed to this method the resulting predicate
     *         will always return {@code false}.
     * @param <T> The type of object the {@link DescribedPredicate predicate} applies to
     */
    @PublicAPI(usage = ACCESS)
    public static <T> DescribedPredicate<T> and(Iterable<? extends DescribedPredicate<? super T>> predicates) {
        return joinPredicates(predicates, (a, b) -> a.and(b));
    }

    /**
     * @see #or(Iterable)
     */
    @SafeVarargs
    @PublicAPI(usage = ACCESS)
    @SuppressWarnings("RedundantTypeArguments") // Unfortunately not really redundant with JDK 8 :-(
    public static <T> DescribedPredicate<T> or(DescribedPredicate<? super T>... predicates) {
        return or(ImmutableList.<DescribedPredicate<? super T>>copyOf(predicates));
    }

    /**
     * @param predicates Any number of {@link DescribedPredicate predicates} to join together via *OR*
     * @return A {@link DescribedPredicate predicate} that returns {@code true}, if any of the supplied
     *         predicates returns {@code true}. Otherwise, it returns {@code false}.
     *         If an empty {@link Iterable} is passed to this method the resulting predicate
     *         will always return {@code false}.
     * @param <T> The type of object the {@link DescribedPredicate predicate} applies to
     */
    @PublicAPI(usage = ACCESS)
    public static <T> DescribedPredicate<T> or(Iterable<? extends DescribedPredicate<? super T>> predicates) {
        return joinPredicates(predicates, (a, b) -> a.or(b));
    }

    // DescribedPredicate is contravariant so the cast is safe
    // Calling .get() is safe because we ensure that we have at least 2 elements ahead of the reduce operation
    @SuppressWarnings({"unchecked", "OptionalGetWithoutIsPresent"})
    private static <T> DescribedPredicate<T> joinPredicates(Iterable<? extends DescribedPredicate<? super T>> predicates, BinaryOperator<DescribedPredicate<T>> predicateJoinOperation) {
        if (isEmpty(predicates)) {
            return alwaysFalse();
        }
        if (size(predicates) == 1) {
            return getOnlyElement(predicates).forSubtype();
        }

        return stream(predicates.spliterator(), false)
                .map(it -> (DescribedPredicate<T>) it)
                .reduce(predicateJoinOperation)
                .get();
    }

    @PublicAPI(usage = ACCESS)
    public static DescribedPredicate<Iterable<?>> empty() {
        return EMPTY;
    }

    @PublicAPI(usage = ACCESS)
    public static <T> DescribedPredicate<Optional<T>> optionalContains(DescribedPredicate<? super T> predicate) {
        return new OptionalContainsPredicate<>(predicate);
    }

    @PublicAPI(usage = ACCESS)
    // OPTIONAL_EMPTY is independent of the concrete type parameter T of Optional<T>
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static <T> DescribedPredicate<Optional<T>> optionalEmpty() {
        return (DescribedPredicate) OPTIONAL_EMPTY;
    }

    /**
     * @param predicate A {@link DescribedPredicate} for the elements of the {@link Iterable}
     * @return A {@link DescribedPredicate predicate} for an {@link Iterable} that returns {@code true}
     *         if and only if at least one element of the {@link Iterable} matches the given element predicate.
     * @param <T> The type of object the {@link DescribedPredicate predicate} applies to
     */
    @PublicAPI(usage = ACCESS)
    public static <T> DescribedPredicate<Iterable<? extends T>> anyElementThat(DescribedPredicate<? super T> predicate) {
        return new AnyElementPredicate<>(predicate);
    }

    /**
     * @param predicate A {@link DescribedPredicate} for the elements of the {@link Iterable}
     * @return A {@link DescribedPredicate predicate} for an {@link Iterable} that returns {@code true}
     *         if and only if all elements of the {@link Iterable} matche the given element predicate.
     * @param <T> The type of object the {@link DescribedPredicate predicate} applies to
     */
    @PublicAPI(usage = ACCESS)
    public static <T> DescribedPredicate<Iterable<T>> allElements(DescribedPredicate<? super T> predicate) {
        return new AllElementsPredicate<>(predicate);
    }

    private static final DescribedPredicate<Iterable<?>> EMPTY = new DescribedPredicate<Iterable<?>>("empty") {
        @Override
        public boolean test(Iterable<?> input) {
            return Iterables.isEmpty(input);
        }
    };
    private static final DescribedPredicate<Optional<?>> OPTIONAL_EMPTY = new DescribedPredicate<Optional<?>>("empty") {
        @Override
        public boolean test(Optional<?> input) {
            return !input.isPresent();
        }
    };

    private static class AsPredicate<T> extends DescribedPredicate<T> {
        private final DescribedPredicate<T> current;

        AsPredicate(DescribedPredicate<T> current, String description, Object... params) {
            super(description, params);
            this.current = current;
        }

        @Override
        public boolean test(T input) {
            return current.test(input);
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
        public boolean test(T input) {
            return current.test(input) && other.test(input);
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
        public boolean test(T input) {
            return current.test(input) || other.test(input);
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
        public boolean test(F input) {
            return current.test(function.apply(input));
        }
    }

    private static class NotPredicate<T> extends DescribedPredicate<T> {
        private final DescribedPredicate<T> predicate;

        NotPredicate(DescribedPredicate<? super T> predicate) {
            super("not " + predicate.getDescription());
            this.predicate = checkNotNull(predicate).forSubtype();
        }

        @Override
        public boolean test(T input) {
            return !predicate.test(input);
        }
    }

    private static class EqualToPredicate<T> extends DescribedPredicate<T> {
        private final T value;

        EqualToPredicate(T value) {
            super("equal to '%s'", value);
            this.value = checkNotNull(value);
        }

        @Override
        public boolean test(T input) {
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
        public boolean test(T input) {
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
        public boolean test(T input) {
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
        public boolean test(T input) {
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
        public boolean test(T input) {
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
        public boolean test(T input) {
            return delegate.test(input);
        }
    }

    private static class AnyElementPredicate<T> extends DescribedPredicate<Iterable<? extends T>> {
        private final DescribedPredicate<T> predicate;

        AnyElementPredicate(DescribedPredicate<? super T> predicate) {
            super("any element that " + predicate.getDescription());
            this.predicate = predicate.forSubtype();
        }

        @Override
        public boolean test(Iterable<? extends T> iterable) {
            for (T javaClass : iterable) {
                if (predicate.test(javaClass)) {
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
            this.predicate = predicate.forSubtype();
        }

        @Override
        public boolean test(Iterable<T> iterable) {
            for (T javaClass : iterable) {
                if (!predicate.test(javaClass)) {
                    return false;
                }
            }
            return true;
        }
    }

    private static class OptionalContainsPredicate<T> extends DescribedPredicate<Optional<T>> {
        private final DescribedPredicate<T> predicate;

        OptionalContainsPredicate(DescribedPredicate<? super T> predicate) {
            super("contains " + predicate.getDescription());
            this.predicate = predicate.forSubtype();
        }

        @Override
        public boolean test(Optional<T> optional) {
            return optional.isPresent() && predicate.test(optional.get());
        }
    }
}
