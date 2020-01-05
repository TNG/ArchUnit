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
package com.tngtech.archunit.core.importer;

import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.INHERITANCE;

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
        /**
         * @deprecated Decided to consistently never use contractions -&gt; use {@link #DO_NOT_INCLUDE_TESTS}
         */
        @Deprecated
        DONT_INCLUDE_TESTS {
            private final DoNotIncludeTests doNotIncludeTests = new DoNotIncludeTests();

            @Override
            public boolean includes(Location location) {
                return doNotIncludeTests.includes(location);
            }
        },
        /**
         * @deprecated Decided to consistently never use contractions -&gt; use {@link #DO_NOT_INCLUDE_JARS}
         */
        @Deprecated
        DONT_INCLUDE_JARS {
            private final DoNotIncludeJars doNotIncludeJars = new DoNotIncludeJars();

            @Override
            public boolean includes(Location location) {
                return doNotIncludeJars.includes(location);
            }
        },
        /**
         * @deprecated Decided to consistently never use contractions -&gt; use {@link #DO_NOT_INCLUDE_ARCHIVES}
         */
        @Deprecated
        DONT_INCLUDE_ARCHIVES {
            private final DoNotIncludeArchives doNotIncludeArchives = new DoNotIncludeArchives();

            @Override
            public boolean includes(Location location) {
                return doNotIncludeArchives.includes(location);
            }
        },
        DO_NOT_INCLUDE_TESTS {
            private final DoNotIncludeTests doNotIncludeTests = new DoNotIncludeTests();

            @Override
            public boolean includes(Location location) {
                return doNotIncludeTests.includes(location);
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
        }
    }

    /**
     * @deprecated Decided to consistently never use contractions -&gt; use {@link DoNotIncludeTests}
     */
    @Deprecated
    final class DontIncludeTests implements ImportOption {
        private final DoNotIncludeTests doNotIncludeTests = new DoNotIncludeTests();

        @Override
        public boolean includes(Location location) {
            return doNotIncludeTests.includes(location);
        }
    }

    /**
     * NOTE: This excludes all class files residing in some directory
     * ../target/test-classes/.., ../build/classes/test/.. or ../build/classes/someLang/test/.. (Maven/Gradle standard).
     * Thus it is just a best guess, how tests can be identified,
     * in other environments, it might be necessary, to implement the correct {@link ImportOption} yourself.
     */
    final class DoNotIncludeTests implements ImportOption {
        private static final Pattern MAVEN_PATTERN = Pattern.compile(".*/target/test-classes/.*");
        private static final Pattern GRADLE_PATTERN = Pattern.compile(".*/build/classes/([^/]+/)?test/.*");
        private static final Pattern INTELLIJ_PATTERN = Pattern.compile(".*/out/test/classes/.*");

        private static final Set<Pattern> EXCLUDED_PATTERN = ImmutableSet.of(MAVEN_PATTERN, GRADLE_PATTERN, INTELLIJ_PATTERN);

        @Override
        public boolean includes(Location location) {
            for (Pattern pattern : EXCLUDED_PATTERN) {
                if (location.matches(pattern)) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * @deprecated Decided to consistently never use contractions -&gt; use {@link DoNotIncludeJars}
     */
    @Deprecated
    final class DontIncludeJars implements ImportOption {
        private final DoNotIncludeJars doNotIncludeJars = new DoNotIncludeJars();

        @Override
        public boolean includes(Location location) {
            return doNotIncludeJars.includes(location);
        }
    }

    final class DoNotIncludeJars implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return !location.isJar();
        }
    }

    /**
     * @deprecated Decided to consistently never use contractions -&gt; use {@link DoNotIncludeArchives}
     */
    @Deprecated
    final class DontIncludeArchives implements ImportOption {
        private final DoNotIncludeArchives doNotIncludeArchives = new DoNotIncludeArchives();

        @Override
        public boolean includes(Location location) {
            return doNotIncludeArchives.includes(location);
        }
    }

    final class DoNotIncludeArchives implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return !location.isArchive();
        }
    }
}
