/*
 * Copyright 2014-2021 TNG Technology Consulting GmbH
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
import com.tngtech.archunit.core.domain.JavaType;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasReturnType.Functions.GET_RAW_RETURN_TYPE;

public interface HasReturnType {

    @PublicAPI(usage = ACCESS)
    JavaType getReturnType();

    @PublicAPI(usage = ACCESS)
    JavaClass getRawReturnType();

    final class Predicates {
        private Predicates() {
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasReturnType> rawReturnType(Class<?> returnType) {
            return rawReturnType(returnType.getName());
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasReturnType> rawReturnType(String returnTypeName) {
            return rawReturnType(name(returnTypeName).as(returnTypeName));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasReturnType> rawReturnType(DescribedPredicate<? super JavaClass> predicate) {
            return predicate.onResultOf(GET_RAW_RETURN_TYPE).as("raw return type %s", predicate.getDescription());
        }

    }

    final class Functions {
        private Functions() {
        }

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<HasReturnType, JavaType> GET_RETURN_TYPE = new ChainableFunction<HasReturnType, JavaType>() {
            @Override
            public JavaType apply(HasReturnType input) {
                return input.getReturnType();
            }
        };

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<HasReturnType, JavaClass> GET_RAW_RETURN_TYPE = new ChainableFunction<HasReturnType, JavaClass>() {
            @Override
            public JavaClass apply(HasReturnType input) {
                return input.getRawReturnType();
            }
        };

    }
}
