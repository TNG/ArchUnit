package com.tngtech.archunit.base;

public abstract class ChainableFunction<F, T> implements Function<F, T> {
    public <E> Function<E, T> after(final Function<E, ? extends F> function) {
        return new Function<E, T>() {
            @Override
            public T apply(E input) {
                return ChainableFunction.this.apply(function.apply(input));
            }
        };
    }
}
