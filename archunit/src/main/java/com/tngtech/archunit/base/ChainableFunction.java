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
