package com.tngtech.archunit.base;

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

@PublicAPI(usage = INHERITANCE)
public interface Function<F, T> {
    T apply(F input);

    final class Functions {
        private Functions() {
        }

        @PublicAPI(usage = ACCESS)
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
