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

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClassList;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.equalTo;
import static com.tngtech.archunit.core.domain.Formatters.formatMethodParameterTypeNames;
import static com.tngtech.archunit.core.domain.JavaClass.namesOf;
import static com.tngtech.archunit.core.domain.JavaClassList.GET_NAMES;

public interface HasParameterTypes {
    /**
     * @deprecated Use {@link #getRawParameterTypes()} instead
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    JavaClassList getParameters();

    @PublicAPI(usage = ACCESS)
    JavaClassList getRawParameterTypes();

    final class Predicates {
        private Predicates() {
        }

        /**
         * @deprecated Use {@link #rawParameterTypes(Class[])}
         */
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasParameterTypes> parameterTypes(final Class<?>... types) {
            return adjustDeprecatedDescription(rawParameterTypes(types));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasParameterTypes> rawParameterTypes(final Class<?>... types) {
            return rawParameterTypes(namesOf(types));
        }

        /**
         * @deprecated Use {@link #rawParameterTypes(String[])}
         */
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasParameterTypes> parameterTypes(final String... types) {
            return adjustDeprecatedDescription(rawParameterTypes(types));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasParameterTypes> rawParameterTypes(final String... types) {
            return rawParameterTypes(ImmutableList.copyOf(types));
        }

        /**
         * @deprecated Use {@link #rawParameterTypes(List)}
         */
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasParameterTypes> parameterTypes(final List<String> typeNames) {
            return adjustDeprecatedDescription(rawParameterTypes(typeNames));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasParameterTypes> rawParameterTypes(final List<String> typeNames) {
            return new RawParameterTypesPredicate(equalTo(typeNames).onResultOf(GET_NAMES)
                    .as("[%s]", formatMethodParameterTypeNames(typeNames)));
        }

        /**
         * @deprecated Use {@link #rawParameterTypes(DescribedPredicate)}
         */
        @Deprecated
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasParameterTypes> parameterTypes(final DescribedPredicate<? super JavaClassList> predicate) {
            return adjustDeprecatedDescription(new RawParameterTypesPredicate(predicate));
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasParameterTypes> rawParameterTypes(final DescribedPredicate<? super List<JavaClass>> predicate) {
            return new RawParameterTypesPredicate(predicate);
        }

        private static DescribedPredicate<HasParameterTypes> adjustDeprecatedDescription(DescribedPredicate<HasParameterTypes> predicate) {
            return predicate.as(predicate.getDescription().replace("raw parameter", "parameter"));
        }

        private static class RawParameterTypesPredicate extends DescribedPredicate<HasParameterTypes> {
            private final DescribedPredicate<? super JavaClassList> predicate;

            RawParameterTypesPredicate(DescribedPredicate<? super JavaClassList> predicate) {
                super("raw parameter types " + predicate.getDescription());
                this.predicate = predicate;
            }

            @Override
            public boolean apply(HasParameterTypes input) {
                return predicate.apply(input.getRawParameterTypes());
            }
        }
    }
}
