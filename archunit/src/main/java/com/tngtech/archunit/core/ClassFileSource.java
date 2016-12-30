package com.tngtech.archunit.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.FluentIterable;
import com.tngtech.archunit.core.ArchUnitException.LocationException;

import static com.tngtech.archunit.core.Location.newJarUrl;

interface ClassFileSource extends Iterable<ClassFileLocation> {
    class FromFilePath extends SimpleFileVisitor<Path> implements ClassFileSource {
        private static final PathMatcher classMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.class");
        private final Set<ClassFileLocation> classFileLocations = new HashSet<>();

        FromFilePath(Path path) {
            if (path.toFile().exists()) {
                try {
                    Files.walkFileTree(path, this);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public Iterator<ClassFileLocation> iterator() {
            return classFileLocations.iterator();
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (classMatcher.matches(file)) {
                classFileLocations.add(new InputStreamSupplierClassFileLocation(file.toUri(), newInputStreamSupplierFor(file)));
            }
            return super.visitFile(file, attrs);
        }

        private Supplier<InputStream> newInputStreamSupplierFor(final Path file) {
            return new InputStreamSupplier() {
                @Override
                InputStream getInputStream() throws IOException {
                    return Files.newInputStream(file);
                }
            };
        }

    }

    class FromJar implements ClassFileSource {
        private final FluentIterable<ClassFileLocation> classFileLocations;

        FromJar(JarURLConnection connection) {
            try {
                JarFile jarFile = connection.getJarFile();
                String prefix = connection.getJarEntry() != null ? connection.getJarEntry().getName() : "";
                classFileLocations = FluentIterable.from(Collections.list(jarFile.entries()))
                        .filter(classFilesBeneath(prefix))
                        .transform(toInputStreamSupplierFrom(connection));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private Predicate<JarEntry> classFilesBeneath(final String prefix) {
            return new Predicate<JarEntry>() {
                @Override
                public boolean apply(JarEntry input) {
                    return input.getName().startsWith(prefix)
                            && input.getName().endsWith(".class");
                }
            };
        }

        private Function<JarEntry, ClassFileLocation> toInputStreamSupplierFrom(final JarURLConnection connection) {
            return new Function<JarEntry, ClassFileLocation>() {
                @Override
                public ClassFileLocation apply(final JarEntry input) {
                    return new InputStreamSupplierClassFileLocation(makeJarUrl(input), new InputStreamSupplier() {
                        @Override
                        InputStream getInputStream() throws IOException {
                            return connection.getJarFile().getInputStream(input);
                        }
                    });
                }

                private URI makeJarUrl(JarEntry input) {
                    try {
                        return new URI(newJarUrl(connection.getJarFileURL()) + input.getName());
                    } catch (URISyntaxException e) {
                        throw new LocationException(e);
                    }
                }
            };
        }

        @Override
        public Iterator<ClassFileLocation> iterator() {
            return classFileLocations.iterator();
        }
    }

    class InputStreamSupplierClassFileLocation implements ClassFileLocation {
        private final URI uri;
        private final Supplier<InputStream> streamSupplier;

        InputStreamSupplierClassFileLocation(URI uri, Supplier<InputStream> streamSupplier) {
            this.uri = uri;
            this.streamSupplier = streamSupplier;
        }

        @Override
        public InputStream openStream() {
            return streamSupplier.get();
        }

        @Override
        public URI getUri() {
            return uri;
        }
    }

    abstract class InputStreamSupplier implements Supplier<InputStream> {
        @Override
        public InputStream get() {
            try {
                return getInputStream();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        abstract InputStream getInputStream() throws IOException;
    }
}
