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

import java.util.regex.Pattern;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface HasName {
    @PublicAPI(usage = ACCESS)
    String getName();

    interface AndFullName extends HasName {
        @PublicAPI(usage = ACCESS)
        String getFullName();
    }

    final class Predicates {
        private Predicates() {
        }

        /**
         * Matches names against a regular expression.
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasName> nameMatching(final String regex) {
            return new NameMatchingPredicate(regex);
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasName> name(final String name) {
            return new NameEqualsPredicate(name);
        }

        private static class NameMatchingPredicate extends DescribedPredicate<HasName> {
            private final Pattern pattern;

            NameMatchingPredicate(String regex) {
                super(String.format("name matching '%s'", regex));
                this.pattern = Pattern.compile(regex);
            }

            @Override
            public boolean apply(HasName input) {
                return pattern.matcher(input.getName()).matches();
            }
        }

        private static class NameEqualsPredicate extends DescribedPredicate<HasName> {
            private final String name;

            NameEqualsPredicate(String name) {
                super(String.format("name '%s'", name));
                this.name = name;
            }

            @Override
            public boolean apply(HasName input) {
                return input.getName().equals(name);
            }
        }
    }

    final class Functions {
        private Functions() {
        }

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<HasName, String> GET_NAME = new ChainableFunction<HasName, String>() {
            @Override
            public String apply(HasName input) {
                return input.getName();
            }
        };
    }
}
