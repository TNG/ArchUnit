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
package com.tngtech.archunit.core.domain.properties;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.domain.properties.HasName.Functions.GET_NAME;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;

public interface HasType {

    /**
     * @deprecated Use {@link #getRawType()} instead
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    JavaClass getType();

    @PublicAPI(usage = ACCESS)
    JavaClass getRawType();

    final class Predicates {
        private Predicates() {
        }

        /**
         * @deprecated Use {@link #rawType(Class)} instead
         */
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasType> type(Class<?> type) {
            return adjustDeprecatedDescription(rawType(type));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasType> rawType(Class<?> type) {
            return rawType(type.getName());
        }

        /**
         * @deprecated Use {@link #rawType(String)} instead
         */
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasType> type(String typeName) {
            return adjustDeprecatedDescription(rawType(typeName));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasType> rawType(String typeName) {
            return rawType(GET_NAME.is(equalTo(typeName))).as("raw type " + typeName);
        }

        /**
         * @deprecated Use {@link #rawType(DescribedPredicate)} instead
         */
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasType> type(DescribedPredicate<? super JavaClass> predicate) {
            return adjustDeprecatedDescription(rawType(predicate));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasType> rawType(DescribedPredicate<? super JavaClass> predicate) {
            return GET_RAW_TYPE.is(predicate).as("raw type " + predicate.getDescription());
        }

        private static DescribedPredicate<HasType> adjustDeprecatedDescription(DescribedPredicate<HasType> predicate) {
            return predicate.as(predicate.getDescription().replace("raw type", "type"));
        }
    }

    final class Functions {
        private Functions() {
        }

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<HasType, JavaClass> GET_RAW_TYPE = new ChainableFunction<HasType, JavaClass>() {
            @Override
            public JavaClass apply(HasType input) {
                return input.getRawType();
            }
        };

        /**
         * @deprecated Use {@link HasType.Functions#GET_RAW_TYPE} instead
         */
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<HasType, JavaClass> GET_TYPE = GET_RAW_TYPE;
    }
}
