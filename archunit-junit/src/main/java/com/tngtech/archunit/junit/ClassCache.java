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
package com.tngtech.archunit.junit;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.ArchUnitException.ReflectionException;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.ImportOptions;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.core.importer.Locations;

class ClassCache {
    @VisibleForTesting
    final Map<Class<?>, JavaClasses> cachedByTest = new ConcurrentHashMap<>();
    private final LoadingCache<LocationsKey, LazyJavaClasses> cachedByLocations =
            CacheBuilder.newBuilder().softValues().build(new CacheLoader<LocationsKey, LazyJavaClasses>() {
                @Override
                public LazyJavaClasses load(LocationsKey key) throws Exception {
                    return new LazyJavaClasses(key.locations, key.importOptionTypes);
                }
            });

    private CacheClassFileImporter cacheClassFileImporter = new CacheClassFileImporter();

    JavaClasses getClassesToAnalyzeFor(Class<?> testClass) {
        checkArgument(testClass);

        if (cachedByTest.containsKey(testClass)) {
            return cachedByTest.get(testClass);
        }

        LocationsKey locations = locationsToImport(testClass);
        JavaClasses classes = cachedByLocations.getUnchecked(locations).get();
        cachedByTest.put(testClass, classes);
        return classes;
    }

    private LocationsKey locationsToImport(Class<?> testClass) {
        AnalyzeClasses analyzeClasses = testClass.getAnnotation(AnalyzeClasses.class);
        Set<Location> declaredLocations = ImmutableSet.<Location>builder()
                .addAll(getLocationsOfPackages(analyzeClasses))
                .addAll(getLocationsOfProviders(testClass, analyzeClasses))
                .build();
        Set<Location> locations = declaredLocations.isEmpty() ? Locations.inClassPath() : declaredLocations;
        return new LocationsKey(analyzeClasses.importOptions(), locations);
    }

    private Set<Location> getLocationsOfPackages(AnalyzeClasses analyzeClasses) {
        Set<String> packages = ImmutableSet.<String>builder()
                .add(analyzeClasses.packages())
                .addAll(toPackageStrings(analyzeClasses.packagesOf()))
                .build();
        return locationsOf(packages);
    }

    private Set<Location> getLocationsOfProviders(Class<?> testClass, AnalyzeClasses analyzeClasses) {
        Set<Location> result = new HashSet<>();
        for (Class<? extends LocationProvider> providerClass : analyzeClasses.locations()) {
            result.addAll(tryCreate(providerClass).get(testClass));
        }
        return result;
    }

    private LocationProvider tryCreate(Class<? extends LocationProvider> providerClass) {
        try {
            return newInstanceOf(providerClass);
        } catch (RuntimeException e) {
            String message = String.format(
                    "Failed to create %s. It must be accessible and provide a public default constructor",
                    LocationProvider.class.getSimpleName());
            throw new ArchTestExecutionException(message, e);
        }
    }

    private Set<String> toPackageStrings(Class<?>[] classes) {
        ImmutableSet.Builder<String> result = ImmutableSet.builder();
        for (Class<?> clazz : classes) {
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

    private static <T> T newInstanceOf(Class<T> type) {
        try {
            return type.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ReflectionException(e);
        }
    }

    private void checkArgument(Class<?> testClass) {
        if (testClass.getAnnotation(AnalyzeClasses.class) == null) {
            throw new IllegalArgumentException(String.format("Class %s must be annotated with @%s",
                    testClass.getSimpleName(), AnalyzeClasses.class.getSimpleName()));
        }
    }

    void clear(Class<?> testClass) {
        cachedByTest.remove(testClass);
    }

    private class LazyJavaClasses {
        private final Set<Location> locations;
        private final Set<Class<? extends ImportOption>> importOptionTypes;
        private volatile JavaClasses javaClasses;

        private LazyJavaClasses(Set<Location> locations, Set<Class<? extends ImportOption>> importOptionTypes) {
            this.locations = locations;
            this.importOptionTypes = importOptionTypes;
        }

        public JavaClasses get() {
            if (javaClasses == null) {
                initialize();
            }
            return javaClasses;
        }

        private synchronized void initialize() {
            if (javaClasses == null) {
                ImportOptions importOptions = new ImportOptions();
                for (Class<? extends ImportOption> optionClass : importOptionTypes) {
                    importOptions = importOptions.with(newInstanceOf(optionClass));
                }
                javaClasses = cacheClassFileImporter.importClasses(importOptions, locations);
            }
        }
    }

    // Used for testing -> that's also the reason it's declared top level
    static class CacheClassFileImporter {
        JavaClasses importClasses(ImportOptions importOptions, Collection<Location> locations) {
            return new ClassFileImporter(importOptions).importLocations(locations);
        }
    }

    private static class LocationsKey {
        private final Set<Class<? extends ImportOption>> importOptionTypes;
        private final Set<Location> locations;

        private LocationsKey(Class<? extends ImportOption>[] importOptionTypes, Set<Location> locations) {
            this.importOptionTypes = ImmutableSet.copyOf(importOptionTypes);
            this.locations = locations;
        }

        @Override
        public int hashCode() {
            return Objects.hash(importOptionTypes, locations);
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
            return Objects.equals(this.importOptionTypes, other.importOptionTypes)
                    && Objects.equals(this.locations, other.locations);
        }
    }
}
