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
package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ArchUnitException.LocationException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public final class Locations {
    private Locations() {
    }

    @PublicAPI(usage = ACCESS)
    public static Set<Location> of(Collection<URL> urls) {
        Set<Location> result = new HashSet<>();
        for (URL url : urls) {
            result.add(Location.of(url));
        }
        return result;
    }

    /**
     * All locations in the classpath that match the supplied package.<br>
     * NOTE: Only works, if the used ClassLoader extends {@link URLClassLoader} (which is true for normal settings)
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
        Set<Location> result = new HashSet<>();
        for (URLClassLoader loader : findAllUrlClassLoadersInContext()) {
            for (URL url : loader.getURLs()) {
                result.add(Location.of(url));
            }
        }
        return result;
    }

    private static String asResourceName(String qualifiedName) {
        return qualifiedName.replace('.', '/');
    }

    private static Set<Location> getLocationsOf(String resourceName) {
        ImmutableSet.Builder<Location> result = ImmutableSet.builder();
        for (URLClassLoader loader : findAllUrlClassLoadersInContext()) {
            result.addAll(getResourceLocations(loader, resourceName));
        }
        return result.build();
    }

    private static Collection<Location> getResourceLocations(URLClassLoader loader, String resourceName) {
        try {
            Set<Location> result = Locations.of(Collections.list(loader.getResources(resourceName)));
            result.addAll(findMissedClassesDueToLackOfPackageJarEntry(result, loader, resourceName));
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
            Set<Location> locationsSoFar, URLClassLoader loader, String resourceName) {

        String searchedJarEntryPrefix = resourceName.endsWith("/") ? resourceName : resourceName + "/";

        NonRedundantJarEntryLocations result = new NonRedundantJarEntryLocations(locationsSoFar);
        for (Location location : jarLocationsOf(loader)) {
            for (JarEntry entry : Collections.list(newJarFile(location).entries())) {
                if (entry.getName().startsWith(searchedJarEntryPrefix)) {
                    result.add(location.append(entry.getName()));
                }
            }
        }
        return result.asCollection();
    }

    private static Set<Location> jarLocationsOf(URLClassLoader loader) {
        return FluentIterable.from(Locations.of(ImmutableSet.copyOf(loader.getURLs())))
                .filter(new Predicate<Location>() {
                    @Override
                    public boolean apply(Location input) {
                        return input.isJar();
                    }
                }).toSet();
    }

    private static Set<URLClassLoader> findAllUrlClassLoadersInContext() {
        return ImmutableSet.<URLClassLoader>builder()
                .addAll(findUrlClassLoadersInHierarchy(Thread.currentThread().getContextClassLoader()))
                .addAll(findUrlClassLoadersInHierarchy(Locations.class.getClassLoader()))
                .build();
    }

    private static Set<URLClassLoader> findUrlClassLoadersInHierarchy(ClassLoader loader) {
        Set<URLClassLoader> result = new HashSet<>();
        while (loader != null) {
            if (loader instanceof URLClassLoader) {
                result.add((URLClassLoader) loader);
            }
            loader = loader.getParent();
        }
        return result;
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

    private static class NonRedundantJarEntryLocations {
        private final Set<Location> locations = new HashSet<>();

        NonRedundantJarEntryLocations(Set<Location> locationsSoFar) {
            for (Location location : locationsSoFar) {
                if (location.isJar()) {
                    locations.add(location);
                }
            }
        }

        void add(Location location) {
            checkArgument(location.isJar());
            if (isRedundant(location)) {
                return;
            }
            for (Iterator<Location> iterator = locations.iterator(); iterator.hasNext(); ) {
                if (iterator.next().startsWith(location)) {
                    iterator.remove();
                }
            }
            locations.add(location);
        }

        private boolean isRedundant(Location location) {
            for (Location stored : locations) {
                if (location.startsWith(stored)) {
                    return true;
                }
            }
            return false;
        }

        Collection<Location> asCollection() {
            return locations;
        }
    }
}
