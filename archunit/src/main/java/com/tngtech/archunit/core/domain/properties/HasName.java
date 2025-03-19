/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
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
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.base.DescribedPredicate;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.properties.HasName.Utils.namesOf;

@PublicAPI(usage = ACCESS)
public interface HasName {
    @PublicAPI(usage = ACCESS)
    String getName();

    @PublicAPI(usage = ACCESS)
    interface AndFullName extends HasName {
        /**
         * @return The full name of the given object. Varies by context, for details consult Javadoc of the concrete subclass.
         */
        @PublicAPI(usage = ACCESS)
        String getFullName();

        /**
         * Predefined {@link DescribedPredicate predicates} targeting objects that implement {@link HasName.AndFullName}
         */
        @PublicAPI(usage = ACCESS)
        final class Predicates {
            private Predicates() {
            }

            @PublicAPI(usage = ACCESS)
            public static DescribedPredicate<HasName.AndFullName> fullName(String fullName) {
                return new FullNameEqualsPredicate(fullName);
            }

            /**
             * Matches full names against a regular expression.
             */
            @PublicAPI(usage = ACCESS)
            public static DescribedPredicate<HasName.AndFullName> fullNameMatching(String regex) {
                return new FullNameMatchingPredicate(regex);
            }

            private static class FullNameEqualsPredicate extends DescribedPredicate<HasName.AndFullName> {
                private final String fullName;

                FullNameEqualsPredicate(String fullName) {
                    super(String.format("full name '%s'", fullName));
                    this.fullName = fullName;
                }

                @Override
                public boolean test(HasName.AndFullName input) {
                    return input.getFullName().equals(fullName);
                }
            }

            private static class FullNameMatchingPredicate extends DescribedPredicate<HasName.AndFullName> {
                private final Pattern pattern;

                FullNameMatchingPredicate(String regex) {
                    super(String.format("full name matching '%s'", regex));
                    this.pattern = Pattern.compile(regex);
                }

                @Override
                public boolean test(HasName.AndFullName input) {
                    return pattern.matcher(input.getFullName()).matches();
                }
            }
        }

        @PublicAPI(usage = ACCESS)
        final class Functions {
            private Functions() {
            }

            @PublicAPI(usage = ACCESS)
            public static final ChainableFunction<HasName.AndFullName, String> GET_FULL_NAME = new ChainableFunction<HasName.AndFullName, String>() {
                @Override
                public String apply(HasName.AndFullName input) {
                    return input.getFullName();
                }
            };
        }
    }

    /**
     * Predefined {@link DescribedPredicate predicates} targeting objects that implement {@link HasName}
     */
    @PublicAPI(usage = ACCESS)
    final class Predicates {
        private Predicates() {
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasName> name(String name) {
            return new NameEqualsPredicate(name);
        }

        /**
         * Matches names against a regular expression.
         */
        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasName> nameMatching(String regex) {
            return new NameMatchingPredicate(regex);
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasName> nameStartingWith(String prefix) {
            return new NameStartingWithPredicate(prefix);
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasName> nameContaining(String infix) {
            return new NameContainingPredicate(infix);
        }

        @PublicAPI(usage = ACCESS)
        public static DescribedPredicate<HasName> nameEndingWith(String postfix) {
            return new NameEndingWithPredicate(postfix);
        }

        private static class NameEqualsPredicate extends DescribedPredicate<HasName> {
            private final String name;

            NameEqualsPredicate(String name) {
                super(String.format("name '%s'", name));
                this.name = name;
            }

            @Override
            public boolean test(HasName input) {
                return input.getName().equals(name);
            }
        }

        private static class NameMatchingPredicate extends DescribedPredicate<HasName> {
            private final Pattern pattern;

            NameMatchingPredicate(String regex) {
                super(String.format("name matching '%s'", regex));
                this.pattern = Pattern.compile(regex);
            }

            @Override
            public boolean test(HasName input) {
                return pattern.matcher(input.getName()).matches();
            }
        }

        private static class NameStartingWithPredicate extends DescribedPredicate<HasName> {
            private final String prefix;

            NameStartingWithPredicate(String prefix) {
                super(String.format("name starting with '%s'", prefix));
                this.prefix = prefix;
            }

            @Override
            public boolean test(HasName input) {
                return input.getName().startsWith(prefix);
            }

        }

        private static class NameContainingPredicate extends DescribedPredicate<HasName> {
            private final String infix;

            NameContainingPredicate(String infix) {
                super(String.format("name containing '%s'", infix));
                this.infix = infix;
            }

            @Override
            public boolean test(HasName input) {
                return input.getName().contains(infix);
            }
        }

        private static class NameEndingWithPredicate extends DescribedPredicate<HasName> {
            private final String suffix;

            NameEndingWithPredicate(String suffix) {
                super(String.format("name ending with '%s'", suffix));
                this.suffix = suffix;
            }

            @Override
            public boolean test(HasName input) {
                return input.getName().endsWith(suffix);
            }
        }
    }

    /**
     * Predefined {@link ChainableFunction functions} to transform {@link HasName}.
     */
    @PublicAPI(usage = ACCESS)
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

        @PublicAPI(usage = ACCESS)
        public static final ChainableFunction<List<? extends HasName>, List<String>> GET_NAMES = new ChainableFunction<List<? extends HasName>, List<String>>() {
            @Override
            public List<String> apply(List<? extends HasName> input) {
                return namesOf(input);
            }
        };
    }

    @PublicAPI(usage = ACCESS)
    final class Utils {
        private Utils() {
        }

        @PublicAPI(usage = ACCESS)
        public static List<String> namesOf(HasName... hasNames) {
            return namesOf(ImmutableList.copyOf(hasNames));
        }

        @PublicAPI(usage = ACCESS)
        public static List<String> namesOf(Iterable<? extends HasName> hasNames) {
            ImmutableList.Builder<String> result = ImmutableList.builder();
            for (HasName paramType : hasNames) {
                result.add(paramType.getName());
            }
            return result.build();
        }
    }
}
