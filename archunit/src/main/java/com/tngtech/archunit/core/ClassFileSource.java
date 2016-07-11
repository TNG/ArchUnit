package com.tngtech.archunit.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
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

interface ClassFileSource extends Iterable<Supplier<InputStream>> {
    class FromFilePath extends SimpleFileVisitor<Path> implements ClassFileSource {
        private static final PathMatcher classMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.class");
        private final Set<Supplier<InputStream>> inputStreams = new HashSet<>();

        public FromFilePath(Path path) {
            if (path.toFile().exists()) {
                try {
                    Files.walkFileTree(path, this);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        @Override
        public Iterator<Supplier<InputStream>> iterator() {
            return inputStreams.iterator();
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (classMatcher.matches(file)) {
                inputStreams.add(newInputStreamSupplierFor(file));
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
        private final FluentIterable<Supplier<InputStream>> inputStreams;

        FromJar(JarURLConnection connection) {
            try {
                JarFile jarFile = connection.getJarFile();
                String prefix = connection.getJarEntry() != null ? connection.getJarEntry().getName() : "";
                inputStreams = FluentIterable.from(Collections.list(jarFile.entries()))
                        .filter(classFilesBeneath(prefix))
                        .transform(toInputStreamSupplierFrom(jarFile));
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

        private Function<JarEntry, Supplier<InputStream>> toInputStreamSupplierFrom(final JarFile jar) {
            return new Function<JarEntry, Supplier<InputStream>>() {
                @Override
                public Supplier<InputStream> apply(final JarEntry input) {
                    return new InputStreamSupplier() {
                        @Override
                        InputStream getInputStream() throws IOException {
                            return jar.getInputStream(input);
                        }
                    };
                }
            };
        }

        @Override
        public Iterator<Supplier<InputStream>> iterator() {
            return inputStreams.iterator();
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
