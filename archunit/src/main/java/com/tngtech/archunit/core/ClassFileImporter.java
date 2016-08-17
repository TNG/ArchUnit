package com.tngtech.archunit.core;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

import com.google.common.base.Supplier;
import com.google.common.collect.Iterables;

import static java.util.Collections.singletonList;

public class ClassFileImporter {
    public JavaClasses importPath(Path path) {
        return new ClassFileProcessor().process(new ClassFileSource.FromFilePath(path));
    }

    public JavaClasses importJar(JarFile jar) {
        return new ClassFileProcessor().process(Location.of(jar).asClassFileSource());
    }

    /**
     * Imports classes from the whole classpath without JARs.
     *
     * @return Imported classes
     */
    public JavaClasses importClasspath() {
        return importClasspath(new ImportOptions().with(PredefinedImportOption.DONT_INCLUDE_JARS));
    }

    public JavaClasses importClasspath(ImportOptions options) {
        Set<Location> locations = new HashSet<>();
        for (Location location : Locations.inClassPath()) {
            if (options.include(location)) {
                locations.add(location);
            }
        }
        return importLocations(locations);
    }

    public JavaClasses importUrl(URL url) {
        return importUrls(singletonList(url));
    }

    public JavaClasses importUrls(Collection<URL> urls) {
        return importLocations(Locations.of(urls));
    }

    public JavaClasses importLocations(Collection<Location> locations) {
        List<ClassFileSource> sources = new ArrayList<>();
        for (Location location : locations) {
            sources.add(classFileSourceFor(location));
        }
        return new ClassFileProcessor().process(unify(sources));
    }

    private ClassFileSource classFileSourceFor(Location input) {
        return input.asClassFileSource();
    }

    private ClassFileSource unify(final List<ClassFileSource> sources) {
        final Iterable<Supplier<InputStream>> concatenatedStreams = Iterables.concat(sources);
        return new ClassFileSource() {
            @Override
            public Iterator<Supplier<InputStream>> iterator() {
                return concatenatedStreams.iterator();
            }
        };
    }

    public static class ImportOptions {
        private final Set<ImportOption> options = new HashSet<>();

        public ImportOptions with(ImportOption option) {
            options.add(option);
            return this;
        }

        public boolean include(Location url) {
            for (ImportOption option : options) {
                if (!option.includes(url)) {
                    return false;
                }
            }
            return true;
        }
    }

    public interface ImportOption {
        boolean includes(Location location);

        class Everything implements ImportOption {
            @Override
            public boolean includes(Location location) {
                return true;
            }
        }
    }

    public enum PredefinedImportOption implements ImportOption {
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

    /**
     * NOTE: This excludes all class files residing in some directory ../test/.. or
     * ../test-classes/.. (Maven/Gradle standard), so don't use this, if you have a package
     * test that you want to import.
     */
    public static class DontIncludeTests implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return !location.contains("/test/") && !location.contains("/test-classes/");
        }
    }

    public static class DontIncludeJars implements ImportOption {
        @Override
        public boolean includes(Location location) {
            return !location.isJar();
        }
    }
}
