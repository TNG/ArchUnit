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
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;

import static com.google.common.collect.Iterables.getOnlyElement;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;

public class ClassFileImporter {
    private final ImportOptions importOptions;

    public ClassFileImporter() {
        this(new ImportOptions());
    }

    public ClassFileImporter(ImportOptions importOptions) {
        this.importOptions = importOptions;
    }

    public ClassFileImporter withImportOption(ImportOption option) {
        return new ClassFileImporter(importOptions.with(option));
    }

    public JavaClasses importPath(Path path) {
        return importLocations(singleton(Location.of(path)));
    }

    public JavaClasses importJar(JarFile jar) {
        return importLocations(singleton(Location.of(jar)));
    }

    /**
     * Imports packages via {@link Locations#ofPackage(String)}
     */
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
    public JavaClasses importClasspath() {
        return importClasspath(new ImportOptions().with(ImportOption.Predefined.DONT_INCLUDE_JARS));
    }

    public JavaClasses importClasspath(ImportOptions options) {
        return new ClassFileImporter(options).importLocations(Locations.inClassPath());
    }

    public JavaClass importClass(Class<?> clazz) {
        return getOnlyElement(importClasses(clazz));
    }

    public JavaClasses importClasses(Class<?>... classes) {
        return importClasses(Arrays.asList(classes));
    }

    public JavaClasses importClasses(Collection<Class<?>> classes) {
        List<URL> urls = new ArrayList<>();
        for (Class<?> clazz : classes) {
            urls.add(getClass().getResource(
                    "/" + clazz.getName().replace(".", "/") + ".class"));
        }
        return importUrls(urls);
    }

    public JavaClasses importUrl(URL url) {
        return importUrls(singletonList(url));
    }

    public JavaClasses importUrls(Collection<URL> urls) {
        return importLocations(Locations.of(urls));
    }

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
