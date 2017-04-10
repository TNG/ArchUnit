package com.tngtech.archunit.junit;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.ArchUnitException.ReflectionException;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.core.importer.Locations;

class ClassCache {
    private final ConcurrentHashMap<Class<?>, JavaClasses> cachedByTest = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<LocationsKey, LazyJavaClasses> cachedByLocations = new ConcurrentHashMap<>();

    private ClassFileImporterFactory classFileImporterFactory = new ClassFileImporterFactory();

    JavaClasses getClassesToAnalyseFor(Class<?> testClass) {
        checkArgument(testClass);

        if (cachedByTest.containsKey(testClass)) {
            return cachedByTest.get(testClass);
        }

        LocationsKey locations = locationsToImport(testClass);
        cachedByLocations.putIfAbsent(locations, new LazyJavaClasses(locations));
        cachedByTest.put(testClass, cachedByLocations.get(locations).get());
        return cachedByLocations.get(locations).get();
    }

    private LocationsKey locationsToImport(Class<?> testClass) {
        AnalyseClasses analyseClasses = testClass.getAnnotation(AnalyseClasses.class);
        Set<String> packages = ImmutableSet.<String>builder()
                .add(analyseClasses.packages())
                .addAll(toPackageStrings(analyseClasses.packagesOf()))
                .build();
        Set<Location> locations = packages.isEmpty() ? Locations.inClassPath() : locationsOf(packages);
        return new LocationsKey(analyseClasses.importOption(), locations);
    }

    private Set<String> toPackageStrings(Class[] classes) {
        ImmutableSet.Builder<String> result = ImmutableSet.builder();
        for (Class clazz : classes) {
            result.add(clazz.getPackage().getName());
        }
        return result.build();
    }

    private Set<Location> locationsOf(Set<String> packages) {
        Set<Location> result = new HashSet<>();
        for (String pkg : packages) {
            result.addAll(Locations.ofPackage(pkg));
        }
        return result;
    }

    // Would be great, if we could just pass the import option on to the ClassFileImporter, but this would be
    // problematic with respect to caching classes for certain Location combinations
    private Set<Location> filter(Set<Location> locations, Class<? extends ImportOption> importOption) {
        ImportOption option = newInstanceOf(importOption);
        Set<Location> result = new HashSet<>();
        for (Location location : locations) {
            if (option.includes(location)) {
                result.add(location);
            }
        }
        return result;
    }

    private static <T> T newInstanceOf(Class<T> type) {
        try {
            return type.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ReflectionException(e);
        }
    }

    private void checkArgument(Class<?> testClass) {
        if (testClass.getAnnotation(AnalyseClasses.class) == null) {
            throw new IllegalArgumentException(String.format("Class %s must be annotated with @%s",
                    testClass.getSimpleName(), AnalyseClasses.class.getSimpleName()));
        }
    }

    private class LazyJavaClasses {
        private final LocationsKey locationsKey;
        private volatile JavaClasses javaClasses;

        private LazyJavaClasses(LocationsKey locationsKey) {
            this.locationsKey = locationsKey;
        }

        public JavaClasses get() {
            if (javaClasses == null) {
                initialize();
            }
            return javaClasses;
        }

        private synchronized void initialize() {
            if (javaClasses == null) {
                ImportOption importOption = newInstanceOf(locationsKey.importOptionClass);
                javaClasses = classFileImporterFactory.create()
                        .withImportOption(importOption)
                        .importLocations(locationsKey.locations);
            }
        }
    }

    // Used for testing -> that's also the reason it's declared top level
    static class ClassFileImporterFactory {
        ClassFileImporter create() {
            return new ClassFileImporter();
        }
    }

    private static class LocationsKey {
        private final Class<? extends ImportOption> importOptionClass;
        private final Set<Location> locations;

        private LocationsKey(Class<? extends ImportOption> importOptionClass, Set<Location> locations) {
            this.importOptionClass = importOptionClass;
            this.locations = locations;
        }

        @Override
        public int hashCode() {
            return Objects.hash(importOptionClass, locations);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final LocationsKey other = (LocationsKey) obj;
            return Objects.equals(this.importOptionClass, other.importOptionClass)
                    && Objects.equals(this.locations, other.locations);
        }
    }
}
