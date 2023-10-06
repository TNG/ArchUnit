/*
 * Copyright 2014-2023 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.lang.syntax;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

import com.tngtech.archunit.Internal;
import com.tngtech.archunit.base.DescribedPredicate;

@Internal
public final class PredicateAggregator<T> implements Predicate<T> {
    private final AddMode<T> addMode;
    private final Optional<DescribedPredicate<T>> predicate;

    public PredicateAggregator() {
        this(AddMode.and(), Optional.empty());
    }

    private PredicateAggregator(AddMode<T> addMode, Optional<DescribedPredicate<T>> predicate) {
        this.addMode = addMode;
        this.predicate = predicate;
    }

    @Override
    public boolean test(T t) {
        return predicate.map(it -> it.test(t)).orElse(true);
    }

    public PredicateAggregator<T> add(DescribedPredicate<? super T> other) {
        return new PredicateAggregator<>(addMode, Optional.of(addMode.apply(predicate, other)));
    }

    public boolean isPresent() {
        return predicate.isPresent();
    }

    public <V> Optional<V> map(Function<DescribedPredicate<T>, V> function) {
        return predicate.map(function);
    }

    public DescribedPredicate<T> get() {
        return predicate.get();
    }

    public PredicateAggregator<T> thatANDs() {
        return new PredicateAggregator<>(AddMode.and(), predicate);
    }

    public PredicateAggregator<T> thatORs() {
        return new PredicateAggregator<>(AddMode.or(), predicate);
    }

    private abstract static class AddMode<T> {
        static <T> AddMode<T> and() {
            return new AddMode<T>() {
                @Override
                DescribedPredicate<T> apply(Optional<DescribedPredicate<T>> first, DescribedPredicate<? super T> other) {
                    DescribedPredicate<T> second = other.forSubtype();
                    return first.isPresent() ? first.get().and(second) : second;
                }
            };
        }

        static <T> AddMode<T> or() {
            return new AddMode<T>() {
                @Override
                DescribedPredicate<T> apply(Optional<DescribedPredicate<T>> first, DescribedPredicate<? super T> other) {
                    DescribedPredicate<T> second = other.forSubtype();
                    return first.isPresent() ? first.get().or(second) : second;
                }
            };
        }

        abstract DescribedPredicate<T> apply(Optional<DescribedPredicate<T>> first, DescribedPredicate<? super T> other);
    }
}
