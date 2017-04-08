package com.tngtech.archunit.core;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.jar.JarFile;

import com.tngtech.archunit.base.ArchUnitException.LocationException;
import com.tngtech.archunit.base.ArchUnitException.UnsupportedUriSchemeException;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public abstract class Location {
    private static final String FILE_SCHEME = "file";
    private static final String JAR_SCHEME = "jar";

    final URI uri;

    private Location(URI uri) {
        this.uri = checkNotNull(uri);
    }

    public URI asURI() {
        return uri;
    }

    public abstract ClassFileSource asClassFileSource(ImportOptions importOptions);

    public boolean contains(String part) {
        return uri.toString().contains(part);
    }

    public boolean isJar() {
        return JAR_SCHEME.equals(uri.getScheme());
    }

    // NOTE: URI behaves strange, if it is a JAR Uri, i.e. jar:file://.../some.jar!/, resolve doesn't work like expected
    public Location append(String relativeURI) {
        if (uri.toString().endsWith("/") && relativeURI.startsWith("/")) {
            relativeURI = relativeURI.substring(1);
        }
        if (!uri.toString().endsWith("/") && !relativeURI.startsWith("/")) {
            relativeURI = "/" + relativeURI;
        }
        return Location.of(URI.create(uri + relativeURI));
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

    public static Location of(URL url) {
        return of(toURI(url));
    }

    public static Location of(URI uri) {
        uri = ensureJarProtocol(uri);
        if (FILE_SCHEME.equals(uri.getScheme())) {
            return new FilePathLocation(uri);
        }
        if (JAR_SCHEME.equals(uri.getScheme())) {
            return new JarFileLocation(uri);
        }
        throw new UnsupportedUriSchemeException(uri);
    }

    private static URI toURI(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new LocationException(e);
        }
    }

    private static URI ensureJarProtocol(URI uri) {
        return !JAR_SCHEME.equals(uri.getScheme()) && uri.getPath().endsWith(".jar") ? newJarUri(uri) : uri;
    }

    public static Location of(JarFile jar) {
        return new JarFileLocation(newJarUri(newFileUri(jar.getName())));
    }

    private static URI newFileUri(String fileName) {
        return URI.create(String.format("%s:%s", FILE_SCHEME, fileName));
    }

    private static URI newJarUri(URI uri) {
        return URI.create(String.format("%s:%s!/", JAR_SCHEME, uri));
    }

    public static Location of(Path path) {
        return new FilePathLocation(path.toUri());
    }

    private static class JarFileLocation extends Location {
        JarFileLocation(URI uri) {
            super(uri);
            checkScheme(JAR_SCHEME, uri);
        }

        @Override
        public ClassFileSource asClassFileSource(ImportOptions importOptions) {
            try {
                return new ClassFileSource.FromJar((JarURLConnection) uri.toURL().openConnection(), importOptions);
            } catch (IOException e) {
                throw new LocationException(e);
            }
        }
    }

    private static class FilePathLocation extends Location {
        FilePathLocation(URI uri) {
            super(uri);
            checkScheme(FILE_SCHEME, uri);
        }

        @Override
        public ClassFileSource asClassFileSource(ImportOptions importOptions) {
            return new ClassFileSource.FromFilePath(Paths.get(uri), importOptions);
        }
    }
}
