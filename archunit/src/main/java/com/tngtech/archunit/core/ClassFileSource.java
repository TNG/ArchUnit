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
import com.google.common.collect.FluentIterable;

interface ClassFileSource extends Iterable<InputStream> {
    class FromFilePath extends SimpleFileVisitor<Path> implements ClassFileSource {
        private static final PathMatcher classMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.class");
        private final Set<InputStream> inputStreams = new HashSet<>();

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
        public Iterator<InputStream> iterator() {
            return inputStreams.iterator();
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            if (classMatcher.matches(file)) {
                inputStreams.add(Files.newInputStream(file));
            }
            return super.visitFile(file, attrs);
        }
    }

    class FromJar implements ClassFileSource {
        private final FluentIterable<InputStream> inputStreams;

        FromJar(JarURLConnection connection) {
            try {
                JarFile jarFile = connection.getJarFile();
                String prefix = connection.getJarEntry() != null ? connection.getJarEntry().getName() : "";
                inputStreams = FluentIterable.from(Collections.list(jarFile.entries()))
                        .filter(classFilesBeneath(prefix))
                        .transform(toInputStreamFrom(jarFile));
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

        private Function<JarEntry, InputStream> toInputStreamFrom(final JarFile jar) {
            return new Function<JarEntry, InputStream>() {
                @Override
                public InputStream apply(JarEntry input) {
                    try {
                        return jar.getInputStream(input);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            };
        }

        @Override
        public Iterator<InputStream> iterator() {
            return inputStreams.iterator();
        }
    }
}
