package com.tngtech.archunit.core;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

public class ClassFileImporter {
    private final ImportOptions importOptions;

    public ClassFileImporter() {
        this(new ImportOptions());
    }

    public ClassFileImporter(ImportOptions importOptions) {
        this.importOptions = importOptions;
    }

    public ClassFileImporter withImportOption(ImportOption option) {
        return new ClassFileImporter(importOptions.with(option));
    }

    public JavaClasses importPath(Path path) {
        return importLocations(singleton(Location.of(path)));
    }

    public JavaClasses importJar(JarFile jar) {
        return importLocations(singleton(Location.of(jar)));
    }

    /**
     * Imports packages via {@link Locations#ofPackage(String)}
     */
    public JavaClasses importPackages(String... packages) {
        Set<Location> locations = new HashSet<>();
        for (String pkg : packages) {
            locations.addAll(Locations.ofPackage(pkg));
        }
        return importLocations(locations);
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
            locations.add(location);
        }
        return new ClassFileImporter(options).importLocations(locations);
    }

    public JavaClass importClass(Class<?> clazz) {
        return getOnlyElement(importClasses(clazz));
    }

    public JavaClasses importClasses(Class<?>... classes) {
        return importClasses(Arrays.asList(classes));
    }

    public JavaClasses importClasses(Collection<Class<?>> classes) {
        List<URL> urls = new ArrayList<>();
        for (Class<?> clazz : classes) {
            urls.add(getClass().getResource(
                    "/" + clazz.getName().replace(".", "/") + ".class"));
        }
        return importUrls(urls);
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
            if (importOptions.include(location)) {
                sources.add(classFileSourceFor(location));
            }
        }
        return new ClassFileProcessor().process(unify(sources));
    }

    private ClassFileSource classFileSourceFor(Location input) {
        return input.asClassFileSource();
    }

    private ClassFileSource unify(final List<ClassFileSource> sources) {
        final Iterable<ClassFileLocation> concatenatedStreams = Iterables.concat(sources);
        return new ClassFileSource() {
            @Override
            public Iterator<ClassFileLocation> iterator() {
                return concatenatedStreams.iterator();
            }
        };
    }

    public static class ImportOptions {
        private final Set<ImportOption> options;

        public ImportOptions() {
            this(Collections.<ImportOption>emptySet());
        }

        private ImportOptions(Set<ImportOption> options) {
            this.options = checkNotNull(options);
        }

        public ImportOptions with(ImportOption option) {
            return new ImportOptions(ImmutableSet.<ImportOption>builder().addAll(options).add(option).build());
        }

        boolean include(Location url) {
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
