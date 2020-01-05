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

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

@PublicAPI(usage = INHERITANCE)
public interface Predicate<T> {
    boolean apply(T input);

    class Defaults {
        private Defaults() {
        }

        @PublicAPI(usage = ACCESS)
        @SuppressWarnings("unchecked")
        public static <T> Predicate<T> alwaysTrue() {
            return (Predicate<T>) ALWAYS_TRUE;
        }

        @PublicAPI(usage = ACCESS)
        @SuppressWarnings("unchecked")
        public static <T> Predicate<T> alwaysFalse() {
            return (Predicate<T>) ALWAYS_FALSE;
        }

        private static final Predicate<Object> ALWAYS_TRUE = new Predicate<Object>() {
            @Override
            public boolean apply(Object input) {
                return true;
            }
        };

        private static final Predicate<Object> ALWAYS_FALSE = new Predicate<Object>() {
            @Override
            public boolean apply(Object input) {
                return false;
            }
        };
    }
}
