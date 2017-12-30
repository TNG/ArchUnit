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
package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ArchUnitException.LocationException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Sets.newHashSet;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.util.Collections.list;

public final class Locations {
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
        for (URL url : UrlSource.Factory.classpath()) {
            result.add(Location.of(url));
        }
        return result.build();
    }

    private static String asResourceName(String qualifiedName) {
        return qualifiedName.replace('.', '/');
    }

    private static Set<Location> getLocationsOf(String resourceName) {
        UrlSource classpath = UrlSource.Factory.classpath();
        return ImmutableSet.copyOf(getResourceLocations(Locations.class.getClassLoader(), resourceName, classpath));
    }

    private static Collection<Location> getResourceLocations(ClassLoader loader, String resourceName, Iterable<URL> classpath) {
        try {
            Set<Location> result = newHashSet(Locations.of(list(loader.getResources(resourceName))));
            result.addAll(findMissedClassesDueToLackOfPackageJarEntry(result, classpath, resourceName));
            return result;
        } catch (IOException e) {
            throw new LocationException(e);
        }
    }

    /**
     * Unfortunately the behavior with JAR files is not completely consistent. Originally,
     * {@link Locations#ofPackage(String)} simply asked all {@link java.net.URLClassLoader}s, for
     * {@link ClassLoader#getResources(String)} of the respective resource name.
     * E.g.
     * <pre><code>importPackage("org.junit") -> getResources("/org/junit")</code></pre>
     * However, this only works, if all respective JARs contain an entry for the folder, which is not always the
     * case. Consider the standard JRE "rt.jar", which does not contain an entry "/java/io", but nonetheless
     * entries like "/java/io/File.class". Thus an import of "java.io", relying on
     * {@link ClassLoader#getResources(String)}, would not import <code>java.io.File</code>.
     */
    private static Collection<Location> findMissedClassesDueToLackOfPackageJarEntry(
            Set<Location> locationsSoFar, Iterable<URL> classpath, String resourceName) {

        Set<Location> locationsToConsider = filterCandidates(locationsSoFar, classpath);
        String searchedJarEntryPrefix = resourceName.endsWith("/") ? resourceName : resourceName + "/";

        Set<Location> result = new HashSet<>();
        for (Location location : locationsToConsider) {
            if (containsEntryWithPrefix(location, searchedJarEntryPrefix)) {
                result.add(location.append(resourceName));
            }
        }
        return result;
    }

    /**
     * We only need to take those URLs that haven't been considered, yet. In the end, if we already have some
     * URL jar:file:///some.jar!/foo/bar, the entry was obviously not missing from the JAR, so we don't need
     * to consider the JAR anymore.
     */
    private static Set<Location> filterCandidates(Set<Location> locationsSoFar, Iterable<URL> allUrls) {
        Set<Location> allLocations = jarLocationsOf(allUrls);
        Set<Location> locationsToConsider = new HashSet<>(allLocations);
        for (Location location : allLocations) {
            for (Location alreadyAdded : locationsSoFar) {
                // location is a base URL, alreadyAdded will be a sub-URL or a base URL as well
                if (alreadyAdded.startsWith(location)) {
                    locationsToConsider.remove(location);
                }
            }
        }
        return locationsToConsider;
    }

    private static boolean containsEntryWithPrefix(Location location, String searchedJarEntryPrefix) {
        for (JarEntry entry : list(newJarFile(location).entries())) {
            if (entry.getName().startsWith(searchedJarEntryPrefix)) {
                return true;
            }
        }
        return false;
    }

    private static Set<Location> jarLocationsOf(Iterable<URL> urls) {
        return FluentIterable.from(Locations.of(urls))
                .filter(new Predicate<Location>() {
                    @Override
                    public boolean apply(Location input) {
                        return input.isJar();
                    }
                }).toSet();
    }

    private static JarFile newJarFile(Location location) {
        try {
            return new JarFile(getFileOfJarLocation(location));
        } catch (IOException e) {
            throw new LocationException(e);
        }
    }

    private static File getFileOfJarLocation(Location location) {
        checkArgument(location.isJar());

        return new File(URI.create(location.asURI().toString()
                .replaceAll("^jar:", "")
                .replaceAll("!/.*", "")));
    }

}
