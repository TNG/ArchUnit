/*
 * Copyright 2017 TNG Technology Consulting GmbH
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


import com.tngtech.archunit.PublicAPI;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

/**
 * A predicate holding a description.
 *
 * @param <T> The type of objects the predicate applies to
 */
@PublicAPI(usage = INHERITANCE)
public abstract class DescribedPredicate<T> {
    private String description;

    public abstract boolean apply(T input);

    public DescribedPredicate(String description, Object... params) {
        checkArgument(description != null, "Description must be set");
        this.description = String.format(description, params);
    }

    public String getDescription() {
        return description;
    }

    public DescribedPredicate<T> as(String description, Object... params) {
        return new DescribedPredicate<T>(description, params) {
            @Override
            public boolean apply(T input) {
                return DescribedPredicate.this.apply(input);
            }
        };
    }

    public DescribedPredicate<T> and(final DescribedPredicate<? super T> other) {
        return new DescribedPredicate<T>(description + " and " + other.getDescription()) {
            @Override
            public boolean apply(T input) {
                return DescribedPredicate.this.apply(input) && other.apply(input);
            }
        };
    }

    public DescribedPredicate<T> or(final DescribedPredicate<? super T> other) {
        return new DescribedPredicate<T>(description + " or " + other.getDescription()) {
            @Override
            public boolean apply(T input) {
                return DescribedPredicate.this.apply(input) || other.apply(input);
            }
        };
    }

    public <F> DescribedPredicate<F> onResultOf(final Function<? super F, ? extends T> function) {
        checkNotNull(function);
        return new DescribedPredicate<F>(description) {
            @Override
            public boolean apply(F input) {
                return DescribedPredicate.this.apply(function.apply(input));
            }
        };
    }

    /**
     * Workaround for the limitations of the Java type system {@code ->} Can't specify this contravariant type at the language level
     */
    @SuppressWarnings("unchecked") // DescribedPredicate is contravariant
    public <U extends T> DescribedPredicate<U> forSubType() {
        return (DescribedPredicate<U>) this;
    }

    @Override
    public String toString() {
        return getDescription();
    }

    @SuppressWarnings("unchecked")
    public static <T> DescribedPredicate<T> alwaysTrue() {
        return (DescribedPredicate<T>) ALWAYS_TRUE;
    }

    private static DescribedPredicate<Object> ALWAYS_TRUE = new DescribedPredicate<Object>("always true") {
        @Override
        public boolean apply(Object input) {
            return true;
        }
    };

    @SuppressWarnings("unchecked")
    public static <T> DescribedPredicate<T> alwaysFalse() {
        return (DescribedPredicate<T>) ALWAYS_FALSE;
    }

    private static final DescribedPredicate<Object> ALWAYS_FALSE = new DescribedPredicate<Object>("always false") {
        @Override
        public boolean apply(Object input) {
            return false;
        }
    };

    public static <T> DescribedPredicate<T> equalTo(final T object) {
        checkNotNull(object);
        return new DescribedPredicate<T>("equal to '%s'", object) {
            @Override
            public boolean apply(T input) {
                return object.equals(input);
            }
        };
    }

    public static <T> DescribedPredicate<T> doesnt(final DescribedPredicate<T> predicate) {
        return not(predicate).as("doesn't %s", predicate.getDescription());
    }

    public static <T> DescribedPredicate<T> dont(final DescribedPredicate<T> predicate) {
        return not(predicate).as("don't %s", predicate.getDescription());
    }

    public static <T> DescribedPredicate<T> not(final DescribedPredicate<T> predicate) {
        checkNotNull(predicate);
        return new DescribedPredicate<T>("not " + predicate.getDescription()) {
            @Override
            public boolean apply(T input) {
                return !predicate.apply(input);
            }
        };
    }
}
