/*
 * Copyright 2018 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.core.domain.properties;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface HasOwner<T> {
    @PublicAPI(usage = ACCESS)
    T getOwner();

    @PublicAPI(usage = ACCESS)
    final class Predicates {
        private Predicates() {
        }

        public static final class With {
            private With() {
            }

            @PublicAPI(usage = ACCESS)
            public static <T> DescribedPredicate<HasOwner<T>> owner(final DescribedPredicate<? super T> predicate) {
                return new OwnerPredicate<>(predicate);
            }
        }

        private static class OwnerPredicate<T> extends DescribedPredicate<HasOwner<T>> {
            private final DescribedPredicate<? super T> predicate;

            OwnerPredicate(DescribedPredicate<? super T> predicate) {
                super("owner " + predicate.getDescription());
                this.predicate = predicate;
            }

            @Override
            public boolean apply(HasOwner<T> input) {
                return predicate.apply(input.getOwner());
            }
        }
    }

    @PublicAPI(usage = ACCESS)
    final class Functions {
        private Functions() {
        }

        public static final class Get {
            private Get() {
            }

            @PublicAPI(usage = ACCESS)
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
