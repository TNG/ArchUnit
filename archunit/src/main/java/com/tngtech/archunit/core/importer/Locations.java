/*
 * Copyright 2019 TNG Technology Consulting GmbH
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

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ArchUnitException.LocationException;
import com.tngtech.archunit.core.InitialConfiguration;

import static com.google.common.collect.Sets.newHashSet;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.util.Collections.list;

public final class Locations {
    private static final InitialConfiguration<LocationResolver> locationResolver = new InitialConfiguration<>();

    static {
        ImportPlugin.Loader.loadForCurrentPlatform().plugInLocationResolver(locationResolver);
    }

    private Locations() {
    }

    @PublicAPI(usage = ACCESS)
    public static Set<Location> of(Iterable<URL> urls) {
        ImmutableSet.Builder<Location> result = ImmutableSet.builder();
        for (URL url : urls) {
            result.add(Location.of(url));
        }
        return result.build();
    }

    /**
     * All locations in the classpath that match the supplied package.
     *
     * @param pkg the package to look for within the classpath
     * @return Locations of all paths that match the supplied package
     */
    @PublicAPI(usage = ACCESS)
    public static Set<Location> ofPackage(String pkg) {
        ImmutableSet.Builder<Location> result = ImmutableSet.builder();
        for (Location location : getLocationsOf(asResourceName(pkg))) {
            result.add(location);
        }
        return result.build();
    }

    @PublicAPI(usage = ACCESS)
    public static Set<Location> ofClass(Class<?> clazz) {
        return getLocationsOf(asResourceName(clazz.getName()) + ".class");
    }

    @PublicAPI(usage = ACCESS)
    public static Set<Location> inClassPath() {
        ImmutableSet.Builder<Location> result = ImmutableSet.builder();
        for (URL url : locationResolver.get().resolveClassPath()) {
            result.add(Location.of(url));
        }
        return result.build();
    }

    private static String asResourceName(String qualifiedName) {
        return qualifiedName.replace('.', '/');
    }

    private static Set<Location> getLocationsOf(String resourceName) {
        UrlSource classpath = locationResolver.get().resolveClassPath();
        NormalizedResourceName normalizedResourceName = NormalizedResourceName.from(resourceName);
        return ImmutableSet.copyOf(getResourceLocations(Locations.class.getClassLoader(), normalizedResourceName, classpath));
    }

    private static Collection<Location> getResourceLocations(ClassLoader loader, NormalizedResourceName resourceName, Iterable<URL> classpath) {
        try {
            Set<Location> result = newHashSet(Locations.of(list(loader.getResources(resourceName.toString()))));
            if (result.isEmpty()) {
                return findMissedClassesDueToLackOfPackageEntry(classpath, resourceName);
            }
            return result;
        } catch (IOException e) {
            throw new LocationException(e);
        }
    }

    /**
     * Unfortunately the behavior with archives is not completely consistent. Originally,
     * {@link Locations#ofPackage(String)} simply asked all {@link java.net.URLClassLoader}s, for
     * {@link ClassLoader#getResources(String)} of the respective resource name.
     * E.g.
     * <pre><code>importPackage("org.junit") -> getResources("/org/junit")</code></pre>
     * However, this only works, if all respective archives contain an entry for the folder, which is not always the
     * case. Consider the standard JRE "rt.jar", which does not contain an entry "/java/io", but nonetheless
     * entries like "/java/io/File.class". Thus an import of "java.io", relying on
     * {@link ClassLoader#getResources(String)}, would not import <code>java.io.File</code>.
     */
    private static Collection<Location> findMissedClassesDueToLackOfPackageEntry(
            Iterable<URL> classpath, NormalizedResourceName resourceName) {
        Set<Location> result = new HashSet<>();
        for (Location location : archiveLocationsOf(classpath)) {
            if (containsEntryWithPrefix(location, resourceName)) {
                result.add(location.append(resourceName.toString()));
            }
        }
        return result;
    }

    private static boolean containsEntryWithPrefix(Location location, NormalizedResourceName searchedJarEntryPrefix) {
        for (NormalizedResourceName name : location.iterateEntries()) {
            if (name.startsWith(searchedJarEntryPrefix)) {
                return true;
            }
        }
        return false;
    }

    private static Set<Location> archiveLocationsOf(Iterable<URL> urls) {
        return FluentIterable.from(Locations.of(urls))
                .filter(new Predicate<Location>() {
                    @Override
                    public boolean apply(Location input) {
                        return input.isArchive();
                    }
                }).toSet();
    }

}
