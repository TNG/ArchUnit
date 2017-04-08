package com.tngtech.archunit.core;

/**
 * Will be evaluated for every class location, to determine if the class should be imported.<br><br>
 * <b>IMPORTANT</b>: For things like caching to work, it's important, that the behavior of any implementation
 * is constant, i.e. throughout the whole run of a test suite, for any {@link Location} x, the result of
 * <code>importOption.includes(x)</code> must return the same value on consecutive calls for EVERY instance
 * of the custom implementation of {@link ImportOption}.<br>
 * In other words, if you for example create a custom implementation of {@link ImportOption},
 * where you look at some test specific file, if a certain class should be imported, this will
 * cause wrong caching (i.e. the second run will assume, the classes are already cached, because it can't
 * be determined, that the {@link ImportOption} would choose different classes to be selected for this run)
 */
public interface ImportOption {
    boolean includes(Location location);

    enum Predefined implements ImportOption {
        /**
         * @see DontIncludeTests
         */
        DONT_INCLUDE_TESTS {
            private final DontIncludeTests dontIncludeTests = new DontIncludeTests();

            @Override
            public boolean includes(Location location) {
                return dontIncludeTests.includes(location);
            }
        },
        DONT_INCLUDE_JARS {
            private DontIncludeJars dontIncludeJars = new DontIncludeJars();

            @Override
            public boolean includes(Location location) {
                return dontIncludeJars.includes(location);
            }
        }
    }

    class Everything implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return true;
        }
    }

    /**
     * NOTE: This excludes all class files residing in some directory ../test/.. or
     * ../test-classes/.. (Maven/Gradle standard), so don't use this, if you have a package
     * test that you want to import.
     */
    class DontIncludeTests implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return !location.contains("/test/") && !location.contains("/test-classes/");
        }
    }

    class DontIncludeJars implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return !location.isJar();
        }
    }
}
