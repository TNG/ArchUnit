package com.tngtech.archunit.core;

import java.util.Map;

import com.google.common.base.Predicate;

/**
 * NOTE: We keep Google Guava out of the public API and use the Gradle Shadow plugin to repackage the internally
 * used Guava classes. This ensures they don't clash with other versions of Guava we might encounter while
 * scanning classes from the classpath.
 */
public class Guava {
    private static <T> Predicate<T> toGuava(final DescribedPredicate<T> predicate) {
        return new Predicate<T>() {
            @Override
            public boolean apply(T input) {
                return predicate.apply(input);
            }
        };
    }

    public static class Maps {
        public static <K, V> Map<K, V> filterValues(Map<K, V> map, DescribedPredicate<? super V> predicate) {
            return com.google.common.collect.Maps.filterValues(map, toGuava(predicate));
        }
    }

    public static class Iterables {
        public static <T> Iterable<T> filter(Iterable<T> iterable, DescribedPredicate<? super T> predicate) {
            return com.google.common.collect.Iterables.filter(iterable, toGuava(predicate));
        }
    }
}
