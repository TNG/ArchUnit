/*
 * Copyright 2017 TNG Technology Consulting GmbH
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.ArchUnitException.ReflectionException;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.core.importer.Locations;

class ClassCache {
    private final ConcurrentHashMap<Class<?>, JavaClasses> cachedByTest = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<LocationsKey, LazyJavaClasses> cachedByLocations = new ConcurrentHashMap<>();

    private CacheClassFileImporter cacheClassFileImporter = new CacheClassFileImporter();

    JavaClasses getClassesToAnalyzeFor(Class<?> testClass) {
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
        AnalyzeClasses analyzeClasses = testClass.getAnnotation(AnalyzeClasses.class);
        Set<String> packages = ImmutableSet.<String>builder()
                .add(analyzeClasses.packages())
                .addAll(toPackageStrings(analyzeClasses.packagesOf()))
                .build();
        Set<Location> locations = packages.isEmpty() ? Locations.inClassPath() : locationsOf(packages);
        return new LocationsKey(analyzeClasses.importOption(), locations);
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
        if (testClass.getAnnotation(AnalyzeClasses.class) == null) {
            throw new IllegalArgumentException(String.format("Class %s must be annotated with @%s",
                    testClass.getSimpleName(), AnalyzeClasses.class.getSimpleName()));
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
                javaClasses = cacheClassFileImporter.importClasses(importOption, locationsKey.locations);
            }
        }
    }

    // Used for testing -> that's also the reason it's declared top level
    static class CacheClassFileImporter {
        JavaClasses importClasses(ImportOption importOption, Collection<Location> locations) {
            return new ClassFileImporter().withImportOption(importOption).importLocations(locations);
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
