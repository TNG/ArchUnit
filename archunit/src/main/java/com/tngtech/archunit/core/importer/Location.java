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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ArchUnitException.LocationException;
import com.tngtech.archunit.base.ArchUnitException.UnsupportedUriSchemeException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Handles various forms of location, from where classes can be imported, in a consistent way. Any location
 * will be treated like an {@link URI}, thus there won't be any platform dependent file separator problems,
 * or similar.
 */
public abstract class Location {
    private static final InitialConfiguration<Set<Factory>> factories = new InitialConfiguration<>();

    static {
        factories.set(ImmutableSet.of(
                new JarFileLocationFactory(),
                new FilePathLocationFactory()
        ));
    }

    final URI uri;

    private Location(URI uri) {
        this.uri = checkNotNull(uri);
    }

    @PublicAPI(usage = ACCESS)
    public URI asURI() {
        return uri;
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

    // NOTE: URI behaves strange, if it is a JAR Uri, i.e. jar:file://.../some.jar!/, resolve doesn't work like expected
    Location append(String relativeURI) {
        if (uri.toString().endsWith("/") && relativeURI.startsWith("/")) {
            relativeURI = relativeURI.substring(1);
        }
        if (!uri.toString().endsWith("/") && !relativeURI.startsWith("/")) {
            relativeURI = "/" + relativeURI;
        }
        return Location.of(URI.create(uri + relativeURI));
    }

    boolean startsWith(Location location) {
        return uri.toString().startsWith(location.asURI().toString());
    }

    void checkScheme(String scheme, URI uri) {
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
        return new JarFileLocation(jar);
    }

    @PublicAPI(usage = ACCESS)
    public static Location of(Path path) {
        return new FilePathLocation(path.toUri());
    }

    private static URI toURI(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new LocationException(e);
        }
    }

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
            return new JarFileLocation(uri);
        }
    }

    static class FilePathLocationFactory implements Factory {
        @Override
        public boolean supports(String scheme) {
            return FilePathLocation.SCHEME.equals(scheme);
        }

        @Override
        public Location create(URI uri) {
            return new FilePathLocation(uri);
        }
    }

    private static class JarFileLocation extends Location {
        private static final String SCHEME = "jar";

        JarFileLocation(URI uri) {
            super(uri);
            checkScheme(SCHEME, uri);
        }

        JarFileLocation(JarFile jar) {
            this(newJarUri(FilePathLocation.newFileUri(jar.getName())));
        }

        static URI ensureJarProtocol(URI uri) {
            return !SCHEME.equals(uri.getScheme()) && uri.getPath().endsWith(".jar") ? newJarUri(uri) : uri;
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
    }

    private static class FilePathLocation extends Location {
        private static final String SCHEME = "file";

        FilePathLocation(URI uri) {
            super(uri);
            checkScheme(SCHEME, uri);
        }

        static URI newFileUri(String fileName) {
            return new File(fileName).toURI();
        }

        @Override
        ClassFileSource asClassFileSource(ImportOptions importOptions) {
            return new ClassFileSource.FromFilePath(Paths.get(uri), importOptions);
        }

        @Override
        public boolean isJar() {
            return false;
        }
    }
}
