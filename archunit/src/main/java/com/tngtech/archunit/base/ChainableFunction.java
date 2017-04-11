package com.tngtech.archunit.base;

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

@PublicAPI(usage = INHERITANCE)
public abstract class ChainableFunction<F, T> implements Function<F, T> {
    public <E> ChainableFunction<E, T> after(final Function<? super E, ? extends F> function) {
        return new ChainableFunction<E, T>() {
            @Override
            public T apply(E input) {
                return ChainableFunction.this.apply(function.apply(input));
            }
        };
    }

    public <U> ChainableFunction<F, U> then(final Function<? super T, ? extends U> function) {
        return new ChainableFunction<F, U>() {
            @Override
            public U apply(F input) {
                return function.apply(ChainableFunction.this.apply(input));
            }
        };
    }

    public DescribedPredicate<F> is(DescribedPredicate<? super T> predicate) {
        return predicate.onResultOf(this);
    }
}
