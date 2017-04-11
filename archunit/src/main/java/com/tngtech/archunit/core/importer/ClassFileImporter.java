package com.tngtech.archunit.core.importer;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

import com.google.common.collect.Iterables;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

public final class ClassFileImporter {
    private final ImportOptions importOptions;

    @PublicAPI(usage = ACCESS)
    public ClassFileImporter() {
        this(new ImportOptions());
    }

    @PublicAPI(usage = ACCESS)
    public ClassFileImporter(ImportOptions importOptions) {
        this.importOptions = importOptions;
    }

    @PublicAPI(usage = ACCESS)
    public ClassFileImporter withImportOption(ImportOption option) {
        return new ClassFileImporter(importOptions.with(option));
    }

    @PublicAPI(usage = ACCESS)
    public JavaClasses importPath(Path path) {
        return importLocations(singleton(Location.of(path)));
    }

    @PublicAPI(usage = ACCESS)
    public JavaClasses importJar(JarFile jar) {
        return importLocations(singleton(Location.of(jar)));
    }

    /**
     * Imports packages via {@link Locations#ofPackage(String)}
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses importPackages(String... packages) {
        Set<Location> locations = new HashSet<>();
        for (String pkg : packages) {
            locations.addAll(Locations.ofPackage(pkg));
        }
        return importLocations(locations);
    }

    /**
     * Imports classes from the whole classpath without JARs.
     *
     * @return Imported classes
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses importClasspath() {
        return importClasspath(new ImportOptions().with(ImportOption.Predefined.DONT_INCLUDE_JARS));
    }

    @PublicAPI(usage = ACCESS)
    public JavaClasses importClasspath(ImportOptions options) {
        return new ClassFileImporter(options).importLocations(Locations.inClassPath());
    }

    @PublicAPI(usage = ACCESS)
    public JavaClass importClass(Class<?> clazz) {
        return getOnlyElement(importClasses(clazz));
    }

    @PublicAPI(usage = ACCESS)
    public JavaClasses importClasses(Class<?>... classes) {
        return importClasses(Arrays.asList(classes));
    }

    @PublicAPI(usage = ACCESS)
    public JavaClasses importClasses(Collection<Class<?>> classes) {
        List<URL> urls = new ArrayList<>();
        for (Class<?> clazz : classes) {
            urls.add(getClass().getResource(
                    "/" + clazz.getName().replace(".", "/") + ".class"));
        }
        return importUrls(urls);
    }

    @PublicAPI(usage = ACCESS)
    public JavaClasses importUrl(URL url) {
        return importUrls(singletonList(url));
    }

    @PublicAPI(usage = ACCESS)
    public JavaClasses importUrls(Collection<URL> urls) {
        return importLocations(Locations.of(urls));
    }

    @PublicAPI(usage = ACCESS)
    public JavaClasses importLocations(Collection<Location> locations) {
        List<ClassFileSource> sources = new ArrayList<>();
        for (Location location : locations) {
            sources.add(location.asClassFileSource(importOptions));
        }
        return new ClassFileProcessor().process(unify(sources));
    }

    private ClassFileSource unify(final List<ClassFileSource> sources) {
        final Iterable<ClassFileLocation> concatenatedStreams = Iterables.concat(sources);
        return new ClassFileSource() {
            @Override
            public Iterator<ClassFileLocation> iterator() {
                return concatenatedStreams.iterator();
            }
        };
    }
}
