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

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ArchUnitException.LocationException;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public final class Locations {
    private Locations() {
    }

    @PublicAPI(usage = ACCESS)
    public static Collection<Location> of(Collection<URL> urls) {
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
            for (URL url : getResources(loader, resourceName)) {
                result.add(Location.of(url));
            }
        }
        return result.build();
    }

    private static List<URL> getResources(URLClassLoader loader, String resourceName) {
        try {
            return Collections.list(loader.getResources(resourceName));
        } catch (IOException e) {
            throw new LocationException(e);
        }
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
}
