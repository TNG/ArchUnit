package com.tngtech.archunit.core;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.jar.JarFile;

import com.tngtech.archunit.core.ArchUnitException.LocationException;
import com.tngtech.archunit.core.ArchUnitException.UnsupportedUrlProtocolException;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class Location {
    private static final String FILE_PROTOCOL = "file";
    private static final String JAR_PROTOCOL = "jar";

    final URI uri;

    private Location(URL url) {
        try {
            this.uri = url.toURI();
        } catch (URISyntaxException e) {
            throw new LocationException(e);
        }
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
        url = ensureJarProtocol(url);
        if (FILE_PROTOCOL.equals(url.getProtocol())) {
            return new FilePathLocation(url);
        }
        if (JAR_PROTOCOL.equals(url.getProtocol())) {
            return new JarFileLocation(url);
        }
        throw new UnsupportedUrlProtocolException(url);
    }

    private static URL ensureJarProtocol(URL url) {
        return !JAR_PROTOCOL.equals(url.getProtocol()) && url.getFile().endsWith(".jar") ? newJarUrl(url) : url;
    }

    public abstract ClassFileSource asClassFileSource();

    public boolean contains(String part) {
        return uri.toString().contains(part);
    }

    public boolean isJar() {
        return JAR_PROTOCOL.equals(uri.getScheme());
    }

    public static Location of(JarFile jar) {
        return new JarFileLocation(newJarUrl(newURL(String.format("%s:%s", FILE_PROTOCOL, jar.getName()))));
    }

    private static URL newUrl(URI uri, String part) {
        try {
            URL context = uri.toURL();
            String externalForm = context.toExternalForm();
            externalForm = externalForm.endsWith("/") ? externalForm : externalForm + '/';
            return new URL(new URL(externalForm), part);
        } catch (MalformedURLException e) {
            throw new LocationException(e);
        }
    }

    private static URL newJarUrl(URL url) {
        try {
            return new URL(String.format("%s:%s!/", JAR_PROTOCOL, url.toExternalForm()));
        } catch (MalformedURLException e) {
            throw new LocationException(e);
        }
    }

    private static URL newURL(String url) {
        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            throw new LocationException(e);
        }
    }

    private static class JarFileLocation extends Location {
        JarFileLocation(URL url) {
            super(url);
            checkArgument(JAR_PROTOCOL.equals(url.getProtocol()), "URL of %s must have protocol %s", getClass().getSimpleName(), JAR_PROTOCOL);
        }

        @Override
        public ClassFileSource asClassFileSource() {
            try {
                return new ClassFileSource.FromJar((JarURLConnection) uri.toURL().openConnection());
            } catch (IOException e) {
                throw new LocationException(e);
            }
        }

    }

    private static class FilePathLocation extends Location {
        FilePathLocation(URL url) {
            super(url);
            checkArgument(FILE_PROTOCOL.equals(url.getProtocol()), "URL of %s must have protocol %s", getClass().getSimpleName(), FILE_PROTOCOL);
        }

        @Override
        public ClassFileSource asClassFileSource() {
            return new ClassFileSource.FromFilePath(Paths.get(uri));
        }

    }
}
