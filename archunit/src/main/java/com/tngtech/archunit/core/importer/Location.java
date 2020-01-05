/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ArchUnitException.LocationException;
import com.tngtech.archunit.base.ArchUnitException.UnsupportedUriSchemeException;
import com.tngtech.archunit.core.InitialConfiguration;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;

/**
 * Handles various forms of location from where classes can be imported in a consistent way. Any location
 * will be treated like a {@link URI}, thus there will not be any platform dependent file separator problems.<br><br>
 * Examples for locations could be
 * <ul>
 *     <li><code>file:///home/someuser/workspace/myproject/target/classes/myproject/Foo.class</code></li>
 *     <li><code>jar:file:///home/someuser/.m2/repository/myproject/foolib.jar!/myproject/Foo.class</code></li>
 * </ul>
 */
public abstract class Location {
    private static final InitialConfiguration<Set<Factory>> factories = new InitialConfiguration<>();

    static {
        ImportPlugin.Loader.loadForCurrentPlatform().plugInLocationFactories(factories);
    }

    final NormalizedUri uri;

    Location(NormalizedUri uri) {
        this.uri = checkNotNull(uri);
    }

    @PublicAPI(usage = ACCESS)
    public URI asURI() {
        return uri.toURI();
    }

    abstract ClassFileSource asClassFileSource(ImportOptions importOptions);

    /**
     * @param part A part to check the respective location {@link URI} for
     * @return true, if the respective {@link URI} contains the given part
     */
    @PublicAPI(usage = ACCESS)
    public boolean contains(String part) {
        return uri.toString().contains(part);
    }

    /**
     * @param pattern A pattern to compare the respective location {@link URI} against
     * @return true, if the respective {@link URI} matches the given pattern
     */
    @PublicAPI(usage = ACCESS)
    public boolean matches(Pattern pattern) {
        return pattern.matcher(uri.toString()).matches();
    }

    @PublicAPI(usage = ACCESS)
    public abstract boolean isJar();

    /**
     * This is a generalization of {@link #isJar()}. Before JDK 9, the only archives were Jar files,
     * starting with JDK 9, we also have JRTs (the JDK modules).
     * @return true, iff this location represents an archive, like a JAR or JRT
     */
    @PublicAPI(usage = ACCESS)
    public abstract boolean isArchive();

    // NOTE: URI behaves strange, if it is a JAR Uri, i.e. jar:file://.../some.jar!/, resolve does not work like expected
    Location append(String relativeURI) {
        relativeURI = encodeIllegalCharacters(relativeURI);
        if (uri.toString().endsWith("/") && relativeURI.startsWith("/")) {
            relativeURI = relativeURI.substring(1);
        }
        if (!uri.toString().endsWith("/") && !relativeURI.startsWith("/")) {
            relativeURI = "/" + relativeURI;
        }
        return Location.of(URI.create(uri + relativeURI));
    }

    // NOTE: new URI(..) with more than one argument does URL encoding of illegal characters. URLEncoder on the other
    //       hand form-encodes all characters, even '/' which we do not want.
    private String encodeIllegalCharacters(String relativeURI) {
        try {
            return new URI(null, null, relativeURI, null).toString();
        } catch (URISyntaxException e) {
            throw new LocationException(e);
        }
    }

    void checkScheme(String scheme, NormalizedUri uri) {
        checkArgument(scheme.equals(uri.getScheme()),
                "URI %s of %s must have scheme %s, but has %s",
                uri, getClass().getSimpleName(), scheme, uri.getScheme());
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Location other = (Location) obj;
        return Objects.equals(this.uri, other.uri);
    }

    @Override
    public String toString() {
        return "Location{uri=" + uri + '}';
    }

    @PublicAPI(usage = ACCESS)
    public static Location of(URL url) {
        return of(toURI(url));
    }

    @PublicAPI(usage = ACCESS)
    public static Location of(URI uri) {
        uri = JarFileLocation.ensureJarProtocol(uri);
        for (Factory factory : factories.get()) {
            if (factory.supports(uri.getScheme())) {
                return factory.create(uri);
            }
        }
        throw new UnsupportedUriSchemeException(uri);
    }

    @PublicAPI(usage = ACCESS)
    public static Location of(JarFile jar) {
        return JarFileLocation.from(jar);
    }

    @PublicAPI(usage = ACCESS)
    public static Location of(Path path) {
        return FilePathLocation.from(path.toUri());
    }

    static URI toURI(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new LocationException(e);
        }
    }

    /**
     * @return An iterable containing all class file names under this location, e.g. relative file names, Jar entry names, ...
     */
    abstract Iterable<NormalizedResourceName> iterateEntries();

    interface Factory {
        boolean supports(String scheme);

        Location create(URI uri);
    }

    static class JarFileLocationFactory implements Factory {
        @Override
        public boolean supports(String scheme) {
            return JarFileLocation.SCHEME.equals(scheme);
        }

        @Override
        public Location create(URI uri) {
            return JarFileLocation.from(uri);
        }
    }

    static class FilePathLocationFactory implements Factory {
        @Override
        public boolean supports(String scheme) {
            return FilePathLocation.SCHEME.equals(scheme);
        }

        @Override
        public Location create(URI uri) {
            return FilePathLocation.from(uri);
        }
    }

    private static class JarFileLocation extends Location {
        private static final String SCHEME = "jar";

        private JarFileLocation(NormalizedUri uri) {
            super(uri);
            checkScheme(SCHEME, uri);
        }

        static URI ensureJarProtocol(URI uri) {
            return !SCHEME.equals(uri.getScheme()) && uri.getPath().endsWith(".jar") ? newJarUri(uri) : uri;
        }

        static JarFileLocation from(URI uri) {
            checkArgument(uri.toString().contains("!/"), "JAR URI must contain '!/'");
            return new JarFileLocation(NormalizedUri.from(uri));
        }

        static JarFileLocation from(JarFile jar) {
            return from(newJarUri(FilePathLocation.newFileUri(jar.getName())));
        }

        private static URI newJarUri(URI uri) {
            return URI.create(String.format("%s:%s!/", SCHEME, uri));
        }

        @Override
        ClassFileSource asClassFileSource(ImportOptions importOptions) {
            try {
                String[] parts = uri.toString().split("!/", 2);
                return new ClassFileSource.FromJar(new URL(parts[0] + "!/"), parts[1], importOptions);
            } catch (IOException e) {
                throw new LocationException(e);
            }
        }

        @Override
        public boolean isJar() {
            return true;
        }

        @Override
        public boolean isArchive() {
            return true;
        }

        @Override
        Iterable<NormalizedResourceName> iterateEntries() {
            File file = getFileOfJar();
            if (!file.exists()) {
                return emptySet();
            }

            return iterateJarFile(file);
        }

        private File getFileOfJar() {
            return new File(URI.create(uri.toString()
                    .replaceAll("^" + SCHEME + ":", "")
                    .replaceAll("!/.*", "")));
        }

        private Iterable<NormalizedResourceName> iterateJarFile(File fileOfJar) {
            ImmutableList.Builder<NormalizedResourceName> result = ImmutableList.builder();
            String prefix = uri.toString().replaceAll(".*!/", "");
            try (JarFile jarFile = new JarFile(fileOfJar)) {
                result.addAll(readEntries(prefix, jarFile));
            } catch (IOException e) {
                throw new LocationException(e);
            }
            return result.build();
        }

        private List<NormalizedResourceName> readEntries(String prefix, JarFile jarFile) {
            List<NormalizedResourceName> result = new ArrayList<>();
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().startsWith(prefix) && entry.getName().endsWith(".class")) {
                    result.add(NormalizedResourceName.from(entry.getName()));
                }
            }
            return result;
        }
    }

    private static class FilePathLocation extends Location {
        private static final String SCHEME = "file";

        private FilePathLocation(NormalizedUri uri) {
            super(uri);
            checkScheme(SCHEME, uri);
        }

        static URI newFileUri(String fileName) {
            return new File(fileName).toURI();
        }

        static FilePathLocation from(URI uri) {
            return new FilePathLocation(NormalizedUri.from(uri));
        }

        @Override
        ClassFileSource asClassFileSource(ImportOptions importOptions) {
            return new ClassFileSource.FromFilePath(Paths.get(uri.toURI()), importOptions);
        }

        @Override
        public boolean isJar() {
            return false;
        }

        @Override
        public boolean isArchive() {
            return false;
        }

        @Override
        Iterable<NormalizedResourceName> iterateEntries() {
            try {
                return getAllFilesBeneath(uri);
            } catch (IOException e) {
                throw new LocationException(e);
            }
        }

        private List<NormalizedResourceName> getAllFilesBeneath(NormalizedUri uri) throws IOException {
            File rootFile = new File(uri.toURI());
            if (!rootFile.exists()) {
                return emptyList();
            }

            return getAllFilesBeneath(rootFile.toPath());
        }

        private List<NormalizedResourceName> getAllFilesBeneath(final Path root) throws IOException {
            final ImmutableList.Builder<NormalizedResourceName> result = ImmutableList.builder();
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (file.toString().endsWith(".class")) {
                        result.add(NormalizedResourceName.from(root.relativize(file).toString()));
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            return result.build();
        }
    }
}
