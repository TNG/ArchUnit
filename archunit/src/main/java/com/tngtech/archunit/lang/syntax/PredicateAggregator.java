package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.Internal;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Optional;

@Internal
public final class PredicateAggregator<T> {
    private final AddMode<T> addMode;
    private final Optional<DescribedPredicate<T>> predicate;

    public PredicateAggregator() {
        this(AddMode.<T>and(), Optional.<DescribedPredicate<T>>absent());
    }

    private PredicateAggregator(AddMode<T> addMode, Optional<DescribedPredicate<T>> predicate) {
        this.addMode = addMode;
        this.predicate = predicate;
    }

    public PredicateAggregator<T> add(DescribedPredicate<? super T> other) {
        return new PredicateAggregator<>(addMode, Optional.of(addMode.apply(predicate, other)));
    }

    public boolean isPresent() {
        return predicate.isPresent();
    }

    public DescribedPredicate<T> get() {
        return predicate.get();
    }

    public PredicateAggregator<T> thatANDs() {
        return new PredicateAggregator<>(AddMode.<T>and(), predicate);
    }

    public PredicateAggregator<T> thatORs() {
        return new PredicateAggregator<>(AddMode.<T>or(), predicate);
    }

    private abstract static class AddMode<T> {
        static <T> AddMode<T> and() {
            return new AddMode<T>() {
                @Override
                DescribedPredicate<T> apply(Optional<DescribedPredicate<T>> first, DescribedPredicate<? super T> other) {
                    DescribedPredicate<T> second = other.forSubType();
                    return first.isPresent() ? first.get().and(second) : second;
                }
            };
        }

        static <T> AddMode<T> or() {
            return new AddMode<T>() {
                @Override
                DescribedPredicate<T> apply(Optional<DescribedPredicate<T>> first, DescribedPredicate<? super T> other) {
                    DescribedPredicate<T> second = other.forSubType();
                    return first.isPresent() ? first.get().or(second) : second;
                }
            };
        }

        abstract DescribedPredicate<T> apply(Optional<DescribedPredicate<T>> first, DescribedPredicate<? super T> other);
    }
}
