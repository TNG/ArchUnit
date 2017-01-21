package com.tngtech.archunit.lang.syntax;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Optional;

class PredicateAggregator<T> {
    private final Optional<DescribedPredicate<T>> predicate;

    PredicateAggregator() {
        this(Optional.<DescribedPredicate<T>>absent());
    }

    private PredicateAggregator(Optional<DescribedPredicate<T>> predicate) {
        this.predicate = predicate;
    }

    PredicateAggregator<T> and(DescribedPredicate<? super T> furtherPredicate) {
        DescribedPredicate<T> additional = furtherPredicate.forSubType();
        return new PredicateAggregator<>(predicate.isPresent() ?
                Optional.of(predicate.get().and(additional)) :
                Optional.of(additional));
    }

    public boolean isPresent() {
        return predicate.isPresent();
    }

    public DescribedPredicate<T> get() {
        return predicate.get();
    }
}
