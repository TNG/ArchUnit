package com.tngtech.archunit.core.properties;

import com.tngtech.archunit.base.ChainableFunction;

public interface HasOwner<T> {
    T getOwner();

    class Functions {
        public static class Get {
            public static <T> ChainableFunction<HasOwner<T>, T> owner() {
                return new ChainableFunction<HasOwner<T>, T>() {
                    @Override
                    public T apply(HasOwner<T> input) {
                        return input.getOwner();
                    }
                };
            }
        }
    }
}
