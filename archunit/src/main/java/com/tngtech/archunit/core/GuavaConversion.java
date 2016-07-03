package com.tngtech.archunit.core;

import com.google.common.base.Predicate;

/**
 * NOTE: We keep Google Guava out of the public API and use the Gradle Shadow plugin to repackage the internally
 * used Guava classes. This ensures they don't clash with other versions of Guava we might encounter while
 * scanning classes from the classpath.
 */
class GuavaConversion {
    static <T> Predicate<T> toGuava(final FluentPredicate<T> predicate) {
        return new Predicate<T>() {
            @Override
            public boolean apply(T input) {
                return predicate.apply(input);
            }
        };
    }
}
