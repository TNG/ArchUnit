package com.tngtech.archunit.junit;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.ArchUnitException.ReflectionException;
import com.tngtech.archunit.core.ClassFileImporter;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.core.Location;
import com.tngtech.archunit.core.Locations;

class ClassCache {
    private final ConcurrentHashMap<Class<?>, JavaClasses> cachedByTest = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Set<Location>, LazyJavaClasses> cachedByLocations = new ConcurrentHashMap<>();

    private ClassFileImporter classFileImporter = new ClassFileImporter();

    JavaClasses getClassesToAnalyseFor(Class<?> testClass) {
        checkArgument(testClass);

        if (cachedByTest.containsKey(testClass)) {
            return cachedByTest.get(testClass);
        }

        Set<Location> locations = locationsToImport(testClass);
        cachedByLocations.putIfAbsent(locations, new LazyJavaClasses(locations));
        cachedByTest.put(testClass, cachedByLocations.get(locations).get());
        return cachedByLocations.get(locations).get();
    }

    private Set<Location> locationsToImport(Class<?> testClass) {
        AnalyseClasses analyseClasses = testClass.getAnnotation(AnalyseClasses.class);
        Set<String> packages = ImmutableSet.<String>builder()
                .add(analyseClasses.packages())
                .addAll(toPackageStrings(analyseClasses.packagesOf()))
                .build();
        Set<Location> result = packages.isEmpty() ? Locations.inClassPath() : locationsOf(packages);
        return filter(result, analyseClasses.importOption());
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
    private Set<Location> filter(Set<Location> locations, Class<? extends ClassFileImporter.ImportOption> importOption) {
        ClassFileImporter.ImportOption option = newInstanceOf(importOption);
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
        private final Set<Location> locations;
        private volatile JavaClasses javaClasses;

        private LazyJavaClasses(Set<Location> locations) {
            this.locations = locations;
        }

        public JavaClasses get() {
            if (javaClasses == null) {
                initialize();
            }
            return javaClasses;
        }

        private synchronized void initialize() {
            if (javaClasses == null) {
                javaClasses = classFileImporter.importLocations(locations);
            }
        }
    }
}
