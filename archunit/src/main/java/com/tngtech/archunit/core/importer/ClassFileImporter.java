/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
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

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.resolvers.ClassResolver;
import com.tngtech.archunit.core.importer.resolvers.ClassResolverFromClasspath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;

/**
 * The central API to import {@link JavaClasses} from compiled Java class files.
 * Supports various types of {@link Location}, e.g. {@link Path},
 * {@link JarFile} or {@link URL}. The {@link Location Locations} that are scanned can be filtered by
 * {@link #withImportOption(ImportOption)} or {@link #withImportOptions(Collection)},
 * which will then join the {@link ImportOption ImportOptions} using <b>AND</b> semantics.
 * <br><br>
 * Note that information about a class is only complete, if all necessary classes are imported.
 * For example, if class A is imported, and A accesses class B,
 * but class B is not imported, the import will behave according to the configured
 * {@link ClassResolver}. With the default configuration, the importer will use the {@link ClassResolverFromClasspath}
 * to attempt to import missing classes from  the classpath, and by that complete any information necessary for rules.
 * In the above case this would mean, if class A accesses B, but B is missing
 * from the set of imported classes, the importer will try to locate the class on the classpath
 * and then import that class, thus acquiring more information like superclasses and interfaces.
 * However, it will not transitively go on to resolve access targets of these classes.
 * <br><br>
 * The resolution behavior, i.e. what the importer does if a class is missing from the context,
 * can be configured by providing a respective {@link ClassResolver}. To use the simplest {@link ClassResolver} possible,
 * namely one that does not import any further classes, simply configure
 * <pre><code>{@value ArchConfiguration#RESOLVE_MISSING_DEPENDENCIES_FROM_CLASS_PATH}=false</code></pre>
 * within your {@value ArchConfiguration#ARCHUNIT_PROPERTIES_RESOURCE_NAME}.<br>
 * Whenever the configured {@link ClassResolver} does not import a class, the importer will simply create a stub with
 * all information known from the access, i.e. the fully qualified name of B and not much more.
 * In particular the stub class B will miss any information like super classes, interfaces, etc.
 * <br><br>
 * In short, if one wants to write rules about certain properties of classes, it is
 * important to ensure that all relevant classes are imported, even if those might be classes
 * from the JDK (like {@link RuntimeException} or {@link Exception}).
 * <br><br>
 * For further information consult the ArchUnit user guide.
 *
 * @see ArchConfiguration
 */
@PublicAPI(usage = ACCESS)
public final class ClassFileImporter {
    private static final Logger LOG = LoggerFactory.getLogger(ClassFileImporter.class);

    private final ImportOptions importOptions;

    @PublicAPI(usage = ACCESS)
    public ClassFileImporter() {
        this(new ImportOptions());
    }

    @PublicAPI(usage = ACCESS)
    public ClassFileImporter(Collection<ImportOption> importOptions) {
        this(new ImportOptions().with(importOptions));
    }

    private ClassFileImporter(ImportOptions importOptions) {
        this.importOptions = importOptions;
    }

    /**
     * Allows filtering the class files to import by their {@link Location} (i.e. URI). Note that
     * this object will not be modified, but instead a copy with adjusted behavior will be returned.
     *
     * @param option Defines which class file {@link Location} to import
     * @return A {@link ClassFileImporter} which filters the specified class file URIs
     */
    @PublicAPI(usage = ACCESS)
    public ClassFileImporter withImportOption(ImportOption option) {
        return new ClassFileImporter(importOptions.with(option));
    }

    /**
     * Same as {@link #withImportOption(ImportOption)} but takes multiple {@link ImportOption ImportOptions}.
     */
    @PublicAPI(usage = ACCESS)
    public ClassFileImporter withImportOptions(Collection<ImportOption> options) {
        return new ClassFileImporter(importOptions.with(options));
    }

    /**
     * Converts the given {@link String} to a {@link Path} and delegates to {@link #importPaths(Collection)}.
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses importPath(String path) {
        return importPaths(path);
    }

    /**
     * Delegates to {@link #importPaths(Collection)}
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses importPath(Path path) {
        return importPaths(path);
    }

    /**
     * Converts the given {@link String Strings} to {@link Path Paths} and delegates to
     * {@link #importPaths(Collection)}.
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses importPaths(String... paths) {
        return importPaths(stream(paths).map(Paths::get).collect(toSet()));
    }

    /**
     * Delegates to {@link #importPaths(Collection)}
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses importPaths(Path... paths) {
        return importPaths(ImmutableSet.copyOf(paths));
    }

    /**
     * Imports all class files at the given {@link Path file paths}. If a {@link Path} refers to a single
     * class file, that file is imported. If a path refers to a directory,
     * all class files from that directory as well as sub directories will be imported.
     * <br><br>
     * For information about the impact of the imported classes on the evaluation of rules,
     * as well as configuration and details, refer to {@link ClassFileImporter}.
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses importPaths(Collection<Path> paths) {
        return importLocations(paths.stream().map(Location::of).collect(toSet()));
    }

    /**
     * For information about the impact of the imported classes on the evaluation of rules,
     * as well as configuration and details, refer to {@link ClassFileImporter}.
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses importJar(JarFile jar) {
        return importJars(jar);
    }

    /**
     * Delegates to {@link #importJars(Iterable)}
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses importJars(JarFile... jarFiles) {
        return importJars(ImmutableList.copyOf(jarFiles));
    }

    /**
     * Imports all Java class files in the given {@link JarFile JAR files}. To import just parts of JAR archives,
     * it is possible to refer to those classes by {@link URL} (compare {@link #importLocations(Collection)}).<br>
     * For example to import the folder <code>/com/my/subfolder</code> within <code>/path/to/some.jar</code>
     * it is possible to use the URL
     * <pre><code>jar:file:///path/to/some.jar!/com/my/subfolder</code></pre>
     * <br>
     * For information about the impact of the imported classes on the evaluation of rules,
     * as well as configuration and details, refer to {@link ClassFileImporter}.
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses importJars(Iterable<JarFile> jarFiles) {
        Set<Location> locations = new HashSet<>();
        for (JarFile jarFile : jarFiles) {
            locations.add(Location.of(jarFile));
        }
        return importLocations(locations);
    }

    /**
     * Imports packages via {@link Locations#ofPackage(String)}
     * <br><br>
     * For information about the impact of the imported classes on the evaluation of rules,
     * as well as configuration and details, refer to {@link ClassFileImporter}.
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses importPackages(Collection<String> packages) {
        Set<Location> locations = packages.stream()
                .flatMap(pkg -> Locations.ofPackage(pkg).stream())
                .collect(toSet());
        return importLocations(locations);
    }

    /**
     * Imports packages via {@link Locations#ofPackage(String)}
     * <br><br>
     * For information about the impact of the imported classes on the evaluation of rules,
     * as well as configuration and details, refer to {@link ClassFileImporter}.
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses importPackages(String... packages) {
        return importPackages(ImmutableSet.copyOf(packages));
    }

    /**
     * Delegates to {@link #importPackagesOf(Collection)}
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses importPackagesOf(Class<?>... classes) {
        return importPackagesOf(ImmutableSet.copyOf(classes));
    }

    /**
     * Takes the packages of the supplied classes and delegates to {@link #importPackages(String...)}
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses importPackagesOf(Collection<Class<?>> classes) {
        return importPackages(classes.stream().map(clazz -> clazz.getPackage().getName()).collect(toSet()));
    }

    /**
     * Imports classes from the whole classpath.<br>
     * Note that ArchUnit does not distinguish between the classpath and the modulepath for Java &gt;= 9,
     * thus all classes from the classpath or the modulepath will be considered.
     * <br><br>
     * For information about the impact of the imported classes on the evaluation of rules,
     * as well as configuration and details, refer to {@link ClassFileImporter}.
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses importClasspath() {
        return importLocations(Locations.inClassPath());
    }

    /**
     * Delegates to {@link #importClasses(Collection)}
     */
    @PublicAPI(usage = ACCESS)
    public JavaClass importClass(Class<?> clazz) {
        return getOnlyElement(importClasses(clazz));
    }

    /**
     * Delegates to {@link #importClasses(Collection)}
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses importClasses(Class<?>... classes) {
        return importClasses(Arrays.asList(classes));
    }

    /**
     * Imports the class files of the supplied classes from the classpath / modulepath.
     * <br><br>
     * For information about the impact of the imported classes on the evaluation of rules,
     * as well as configuration and details, refer to {@link ClassFileImporter}.
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses importClasses(Collection<Class<?>> classes) {
        Set<Location> locations = classes.stream()
                .flatMap(clazz -> Locations.ofClass(clazz).stream())
                .collect(toSet());
        return importLocations(locations);
    }

    /**
     * Converts the supplied {@link URL} to {@link Location} and delegates to
     * {@link #importLocations(Collection)}
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses importUrl(URL url) {
        return importUrls(singletonList(url));
    }

    /**
     * Converts the supplied {@link URL URLs} to {@link Location locations} and delegates to
     * {@link #importLocations(Collection)}
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses importUrls(Collection<URL> urls) {
        return importLocations(Locations.of(urls));
    }

    /**
     * Imports all class files at the given {@link Location locations}. The behavior will depend on the type of {@link Location},
     * i.e. the target of the underlying URI:
     * <ul>
     *     <li>a single class file =&lt; that file will be imported</li>
     *     <li>a directory =&lt; all class files from that directory as well as sub directories will be imported</li>
     *     <li>a JAR file =&lt; all class files from that archive will be imported</li>
     *     <li>a JAR URI representing a class (e.g. '<code>jar:file:///some.jar!/some/MyClass.class</code>') =&lt; that file will be imported</li>
     *     <li>a JAR URI representing a folder (e.g. '<code>jar:file:///some.jar!/some/pkg</code>') =&lt; all class files from that folder as well as
     *     sub folders within the JAR archive will be imported</li>
     * </ul>
     * For information about the impact of the imported classes on the evaluation of rules,
     * as well as configuration and details, refer to {@link ClassFileImporter}.
     */
    @PublicAPI(usage = ACCESS)
    public JavaClasses importLocations(Collection<Location> locations) {
        List<ClassFileSource> sources = new ArrayList<>();
        for (Location location : locations) {
            tryAdd(sources, location);
        }
        return new ClassFileProcessor().process(unify(sources));
    }

    private void tryAdd(List<ClassFileSource> sources, Location location) {
        try {
            sources.add(location.asClassFileSource(importOptions));
        } catch (Exception e) {
            LOG.warn(String.format("Couldn't derive %s from %s",
                    ClassFileSource.class.getSimpleName(), location), e);
        }
    }

    private ClassFileSource unify(List<ClassFileSource> sources) {
        return Iterables.concat(sources)::iterator;
    }
}
