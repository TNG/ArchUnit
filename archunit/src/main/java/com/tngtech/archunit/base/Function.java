package com.tngtech.archunit.base;

public interface Function<F, T> {
    T apply(F input);

    class Functions {
        public static <T> Function<T, T> identity() {
            return new Function<T, T>() {
                @Override
                public T apply(T input) {
                    return input;
                }
            };
        }
    }
}
