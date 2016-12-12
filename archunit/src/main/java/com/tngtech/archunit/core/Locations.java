package com.tngtech.archunit.core;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.ArchUnitException.LocationException;

public class Locations {
    public static Collection<Location> of(Collection<URL> urls) {
        Set<Location> result = new HashSet<>();
        for (URL url : urls) {
            result.add(Location.of(url));
        }
        return result;
    }

    public static Set<Location> ofPackage(String pkg) {
        ImmutableSet.Builder<Location> result = ImmutableSet.builder();
        for (Location location : getLocationsOf(asResourceName(pkg))) {
            result.add(location);
        }
        return result.build();
    }

    public static Set<Location> ofClass(Class<?> clazz) {
        return getLocationsOf(asResourceName(clazz.getName()) + ".class");
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

    public static Set<Location> inClassPath() {
        Set<Location> result = new HashSet<>();
        for (URLClassLoader loader : findAllUrlClassLoadersInContext()) {
            for (URL url : loader.getURLs()) {
                result.add(Location.of(url));
            }
        }
        return result;
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
