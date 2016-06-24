package com.tngtech.archunit.core;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 * A predicate optionally holding an description denoting how a subset of a collection matching this
 * predicate should be called.
 *
 * @param <T> The type of objects the predicate applies to
 */
public abstract class DescribedPredicate<T> extends FluentPredicate<T> {
    private Optional<String> description = Optional.absent();

    public DescribedPredicate() {
    }

    public DescribedPredicate(String description) {
        this.description = Optional.of(description);
    }

    public Optional<String> getDescription() {
        return description;
    }

    public DescribedPredicate<T> as(String description) {
        return as(Optional.of(description));
    }

    private DescribedPredicate<T> as(Optional<String> description) {
        this.description = description;
        return this;
    }

    @SuppressWarnings("unused")
    public static <T> AlwaysTrue<T> all(Class<T> unused) {
        return new AlwaysTrue<>();
    }

    /**
     * This method is just syntactic sugar, e.g. to write aClass.that(is(special))
     *
     * @param predicate The original predicate
     * @param <T>       The type of the object to decide on
     * @return The original predicate
     */
    public static <T> DescribedPredicate<T> is(Predicate<T> predicate) {
        return predicate instanceof DescribedPredicate ?
                (DescribedPredicate<T>) predicate :
                DescribedPredicate.of(predicate);
    }

    /**
     * This method is just syntactic sugar, e.g. to write classes.that(are(special))
     *
     * @param predicate The original predicate
     * @param <T>       The type of the object to decide on
     * @return The original predicate
     */
    public static <T> DescribedPredicate<T> are(Predicate<T> predicate) {
        return is(predicate);
    }

    public static <T> DescribedPredicate<T> of(final Predicate<T> predicate) {
        return new DescribedPredicate<T>() {
            @Override
            public boolean apply(T input) {
                return predicate.apply(input);
            }
        };
    }

    public static <T> DescribedPredicate<T> not(Predicate<T> predicate) {
        return DescribedPredicate.of(Predicates.not(predicate));
    }

    public <F> DescribedPredicate<F> onResultOf(Function<F, ? extends T> function) {
        return DescribedPredicate.of(Predicates.compose(this, function)).as(description);
    }

    public static class AlwaysTrue<T> extends DescribedPredicate<T> {
        @Override
        public boolean apply(T input) {
            return true;
        }

        public DescribedPredicate<T> that(DescribedPredicate<T> predicate) {
            return predicate;
        }
    }
}
