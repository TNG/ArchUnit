/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.core.importer;

import java.util.regex.Pattern;

import com.google.common.base.Predicate;
import com.tngtech.archunit.PublicAPI;

import static com.google.common.base.Predicates.not;
import static com.google.common.base.Predicates.or;
import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.NO_TEST_LOCATION;
import static com.tngtech.archunit.core.importer.ImportOption.Predefined.TEST_LOCATION;

/**
 * Will be evaluated for every class location, to determine if the class should be imported.<br><br>
 * <b>IMPORTANT</b>: For things like caching to work, it's important that the behavior of any implementation
 * is constant, i.e. throughout the whole run of a test suite, for any {@link Location} x, the result of
 * <code>importOption.includes(x)</code> must return the same value on consecutive calls for EVERY instance
 * of the custom implementation of {@link ImportOption}.<br>
 * In other words, if you for example create a custom implementation of {@link ImportOption},
 * where you look at some test specific file, if a certain class should be imported, this will
 * cause wrong caching (i.e. the second run will assume, the classes are already cached, because it can't
 * be determined that the {@link ImportOption} would choose different classes to be selected for this run)
 */
@PublicAPI(usage = INHERITANCE)
public interface ImportOption {
    boolean includes(Location location);

    enum Predefined implements ImportOption {
        DO_NOT_INCLUDE_TESTS {
            private final DoNotIncludeTests doNotIncludeTests = new DoNotIncludeTests();

            @Override
            public boolean includes(Location location) {
                return doNotIncludeTests.includes(location);
            }
        },
        ONLY_INCLUDE_TESTS {
            private final OnlyIncludeTests onlyIncludeTests = new OnlyIncludeTests();

            @Override
            public boolean includes(Location location) {
                return onlyIncludeTests.includes(location);
            }
        },
        DO_NOT_INCLUDE_JARS {
            private final DoNotIncludeJars doNotIncludeJars = new DoNotIncludeJars();

            @Override
            public boolean includes(Location location) {
                return doNotIncludeJars.includes(location);
            }
        },
        /**
         * Since Java 9 there are JARs and JRTs, this will exclude both
         */
        DO_NOT_INCLUDE_ARCHIVES {
            private final DoNotIncludeArchives doNotIncludeArchives = new DoNotIncludeArchives();

            @Override
            public boolean includes(Location location) {
                return doNotIncludeArchives.includes(location);
            }
        },
        /**
         * @see DoNotIncludePackageInfos
         */
        DO_NOT_INCLUDE_PACKAGE_INFOS {
            private final DoNotIncludePackageInfos doNotIncludePackageInfos = new DoNotIncludePackageInfos();

            @Override
            public boolean includes(Location location) {
                return doNotIncludePackageInfos.includes(location);
            }
        };

        static final PatternPredicate MAVEN_TEST_PATTERN = new PatternPredicate(".*/target/test-classes/.*");
        static final PatternPredicate GRADLE_TEST_PATTERN = new PatternPredicate(".*/build/classes/([^/]+/)?test/.*");
        static final PatternPredicate INTELLIJ_TEST_PATTERN = new PatternPredicate(".*/out/test/classes/.*");
        static final Predicate<Location> TEST_LOCATION = or(MAVEN_TEST_PATTERN, GRADLE_TEST_PATTERN, INTELLIJ_TEST_PATTERN);
        static final Predicate<Location> NO_TEST_LOCATION = not(TEST_LOCATION);

        private static class PatternPredicate implements Predicate<Location> {
            private final Pattern pattern;

            PatternPredicate(String pattern) {
                this.pattern = Pattern.compile(pattern);
            }

            @Override
            @SuppressWarnings("ConstantConditions") // ArchUnit never uses null as a valid parameter
            public boolean apply(Location input) {
                return input.matches(pattern);
            }
        }
    }

    /**
     * Best effort {@link ImportOption} to check rules only on main classes.<br>
     * NOTE: This excludes all class files residing in some directory
     * ../target/test-classes/.., ../build/classes/test/.. or ../build/classes/someLang/test/.. (Maven/Gradle standard).
     * Thus it is just a best guess, how tests can be identified,
     * in other environments, it might be necessary, to implement the correct {@link ImportOption} yourself.
     */
    final class DoNotIncludeTests implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return NO_TEST_LOCATION.apply(location);
        }
    }

    /**
     * Best effort {@link ImportOption} to check rules only on test classes.<br>
     * See {@link DoNotIncludeTests} for limitations of test class identification.
     */
    final class OnlyIncludeTests implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return TEST_LOCATION.apply(location);
        }
    }

    final class DoNotIncludeJars implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return !location.isJar();
        }
    }

    final class DoNotIncludeArchives implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return !location.isArchive();
        }
    }

    /**
     * Excludes {@code package-info.class} files.
     */
    final class DoNotIncludePackageInfos implements ImportOption {
        private static final Pattern PACKAGE_INFO_PATTERN = Pattern.compile(".*package-info\\.class$");

        @Override
        public boolean includes(Location location) {
            return !location.matches(PACKAGE_INFO_PATTERN);
        }
    }
}
