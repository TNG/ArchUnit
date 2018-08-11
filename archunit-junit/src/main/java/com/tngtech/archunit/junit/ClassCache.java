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
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.core.importer.ImportOptions;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.core.importer.Locations;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.junit.CacheMode.FOREVER;
import static com.tngtech.archunit.junit.ReflectionUtils.newInstanceOf;

/**
 * The {@link ClassCache} takes care of caching {@link JavaClasses} between test runs. On the one hand,
 * it caches {@link JavaClasses} between different test runs,
 * on the other hand, it caches {@link JavaClasses} between different test classes,
 * i.e. if two test classes <code>ATest</code> and <code>BTest</code>
 * import the same locations (e.g. packages, URLs, etc.), the imported {@link JavaClasses} from <code>ATest</code> will be
 * reused for <code>BTest</code>. This behavior can be controlled by the supplied {@link CacheMode}.
 * <br><br>
 * Important information regarding performance: The cache uses soft references, meaning that a small heap
 * may dramatically reduce performance, if multiple test classes are executed.
 * The cache will hold imported classes as long as there is sufficient memory, and reuse them, if the same
 * locations (i.e. URLs) are imported.
 */
class ClassCache {
    @VisibleForTesting
    final Map<Class<?>, JavaClasses> cachedByTest = new ConcurrentHashMap<>();
    @VisibleForTesting
    final LoadingCache<LocationsKey, LazyJavaClasses> cachedByLocations =
            CacheBuilder.newBuilder().softValues().build(new CacheLoader<LocationsKey, LazyJavaClasses>() {
                @Override
                public LazyJavaClasses load(LocationsKey key) {
                    return new LazyJavaClasses(key.locations, key.importOptionTypes);
                }
            });

    private CacheClassFileImporter cacheClassFileImporter = new CacheClassFileImporter();

    JavaClasses getClassesToAnalyzeFor(Class<?> testClass, ClassAnalysisRequest classAnalysisRequest) {
        checkNotNull(testClass);
        checkNotNull(classAnalysisRequest);

        if (cachedByTest.containsKey(testClass)) {
            return cachedByTest.get(testClass);
        }

        LocationsKey locations = RequestedLocations.by(classAnalysisRequest, testClass).asKey();

        JavaClasses classes = classAnalysisRequest.getCacheMode() == FOREVER
                ? cachedByLocations.getUnchecked(locations).get()
                : new LazyJavaClasses(locations.locations, locations.importOptionTypes).get();

        cachedByTest.put(testClass, classes);
        return classes;
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

    private abstract static class RequestedLocations {

        abstract LocationsKey asKey();

        private static class All extends RequestedLocations {
            private Class<? extends ImportOption>[] importOptions;

            private All(Class<? extends ImportOption>[] importOptions) {
                this.importOptions = importOptions;
            }

            @Override
            LocationsKey asKey() {
                return new LocationsKey(importOptions, Locations.inClassPath());
            }
        }

        private static class Specific extends RequestedLocations {
            private final ClassAnalysisRequest classAnalysisRequest;
            private final Set<Location> declaredLocations;

            private Specific(ClassAnalysisRequest classAnalysisRequest, Class<?> testClass) {
                this.classAnalysisRequest = classAnalysisRequest;
                declaredLocations = ImmutableSet.<Location>builder()
                        .addAll(getLocationsOfPackages())
                        .addAll(getLocationsOfProviders(testClass))
                        .build();
            }

            private Set<Location> getLocationsOfPackages() {
                Set<String> packages = ImmutableSet.<String>builder()
                        .add(classAnalysisRequest.getPackageNames())
                        .addAll(toPackageStrings(classAnalysisRequest.getPackageRoots()))
                        .build();
                return locationsOf(packages);
            }

            private Set<Location> getLocationsOfProviders(Class<?> testClass) {
                Set<Location> result = new HashSet<>();
                for (Class<? extends LocationProvider> providerClass : classAnalysisRequest.getLocationProviders()) {
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
                            providerClass.getSimpleName());
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

            @Override
            LocationsKey asKey() {
                return new LocationsKey(classAnalysisRequest.getImportOptions(), declaredLocations);
            }
        }

        public static RequestedLocations by(ClassAnalysisRequest classAnalysisRequest, Class<?> testClass) {
            return noSpecificLocationRequested(classAnalysisRequest) ?
                    new All(classAnalysisRequest.getImportOptions()) :
                    new Specific(classAnalysisRequest, testClass);
        }

        private static boolean noSpecificLocationRequested(ClassAnalysisRequest classAnalysisRequest) {
            return classAnalysisRequest.getPackageNames().length == 0
                    && classAnalysisRequest.getPackageRoots().length == 0
                    && classAnalysisRequest.getLocationProviders().length == 0;
        }
    }
}
