/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.base;

import java.util.Map;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.tngtech.archunit.Internal;

/**
 * NOTE: We keep Google Guava out of the public API and use the Gradle Shadow plugin to repackage the internally
 * used Guava classes. This ensures they do not clash with other versions of Guava we might encounter while
 * scanning classes from the classpath.
 */
@Internal
public final class Guava {
    public static <T> Predicate<T> toGuava(final DescribedPredicate<T> predicate) {
        return new Predicate<T>() {
            @Override
            public boolean apply(T input) {
                return predicate.apply(input);
            }
        };
    }

    public static <F, T> Function<F, T> toGuava(final com.tngtech.archunit.base.Function<F, T> function) {
        return new Function<F, T>() {
            @Override
            public T apply(F input) {
                return function.apply(input);
            }
        };
    }

    @Internal
    public static final class Maps {
        public static <K, V> Map<K, V> filterValues(Map<K, V> map, DescribedPredicate<? super V> predicate) {
            return com.google.common.collect.Maps.filterValues(map, toGuava(predicate));
        }
    }

    @Internal
    public static final class Iterables {
        public static <T> Iterable<T> filter(Iterable<T> iterable, DescribedPredicate<? super T> predicate) {
            return com.google.common.collect.Iterables.filter(iterable, toGuava(predicate));
        }
    }
}
