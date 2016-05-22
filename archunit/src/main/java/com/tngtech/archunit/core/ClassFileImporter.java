package com.tngtech.archunit.core;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;
import org.reflections.util.ClasspathHelper;

import static java.util.Collections.singleton;

public class ClassFileImporter {
    public static final String FILE_PROTOCOL = "file";
    public static final String JAR_PROTOCOL = "jar";

    public JavaClasses importPath(Path path) {
        return new ClassFileProcessor().process(new FilePathClassFileSource(path));
    }

    public JavaClasses importJar(JarFile jar) {
        return new ClassFileProcessor().process(new JarClassFileSource(connectionTo(jar)));
    }

    private JarURLConnection connectionTo(JarFile jar) {
        try {
            URL url = new URL(String.format("%s:%s:%s!/", JAR_PROTOCOL, FILE_PROTOCOL, jar.getName()));
            return (JarURLConnection) url.openConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Imports classes from the whole classpath without JARs.
     *
     * @return Imported classes
     */
    public JavaClasses importClasspath() {
        return importClasspath(new ImportOptions().with(PredefinedImportOption.DONT_INCLUDE_JARS));
    }

    public JavaClasses importClasspath(ImportOptions options) {
        Set<URL> urls = new HashSet<>();
        for (URL url : ClasspathHelper.forClassLoader()) {
            if (options.include(url)) {
                urls.add(url);
            }
        }
        return importUrls(urls);
    }

    public JavaClasses importUrl(URL url) {
        return importUrls(singleton(url));
    }

    public JavaClasses importUrls(Collection<URL> urls) {
        List<ClassFileSource> sources = new ArrayList<>();
        for (URL url : urls) {
            sources.add(classFileSourceFor(url));
        }
        return new ClassFileProcessor().process(unify(sources));
    }

    private ClassFileSource classFileSourceFor(URL input) {
        URL url = ensureJarProtokoll(input);
        if (FILE_PROTOCOL.equals(url.getProtocol())) {
            return new FilePathClassFileSource(Paths.get(asUri(url)));
        }
        if (JAR_PROTOCOL.equals(url.getProtocol())) {
            return new JarClassFileSource(connectionFrom(url));
        }
        throw new UnsupportedUrlProtocolException(url);
    }

    private URL ensureJarProtokoll(URL url) {
        return FILE_PROTOCOL.equals(url.getProtocol()) && url.getFile().endsWith(".jar") ? newJarUrl(url) : url;
    }

    private URL newJarUrl(URL url) {
        try {
            return new URL(String.format("%s:%s!/", JAR_PROTOCOL, url.toExternalForm()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private URI asUri(URL url) {
        try {
            return url.toURI();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private JarURLConnection connectionFrom(URL url) {
        try {
            return ((JarURLConnection) url.openConnection());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ClassFileSource unify(final List<ClassFileSource> sources) {
        final Iterable<InputStream> concatenatedStreams = Iterables.concat(sources);
        return new ClassFileSource() {
            @Override
            public Iterator<InputStream> iterator() {
                return concatenatedStreams.iterator();
            }
        };
    }

    private static class FilePathClassFileSource extends SimpleFileVisitor<Path> implements ClassFileSource {
        private static final PathMatcher classMatcher = FileSystems.getDefault().getPathMatcher("glob:**/*.class");
        private final Set<InputStream> inputStreams = new HashSet<>();

        public FilePathClassFileSource(Path path) {
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

    private static class JarClassFileSource implements ClassFileSource {
        private final FluentIterable<InputStream> inputStreams;

        private JarClassFileSource(JarURLConnection url) {
            try {
                JarFile jarFile = url.getJarFile();
                String prefix = url.getJarEntry() != null ? url.getJarEntry().getName() : "";
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

    public static class ImportOptions {
        private final Set<ImportOption> options = new HashSet<>();

        public ImportOptions with(ImportOption option) {
            options.add(option);
            return this;
        }

        public boolean include(URL url) {
            for (ImportOption option : options) {
                if (!option.includes(url)) {
                    return false;
                }
            }
            return true;
        }
    }

    public interface ImportOption {
        boolean includes(URL url);
    }

    public enum PredefinedImportOption implements ImportOption {
        /**
         * NOTE: This excludes all class files residing in some directory ../test/.., so don't use this, if you
         * have a package test that you want to import.
         */
        DONT_INCLUDE_TESTS {
            @Override
            public boolean includes(URL url) {
                return !url.getFile().contains(String.format("%stest%s", File.separator, File.separator));
            }
        },
        DONT_INCLUDE_JARS {
            @Override
            public boolean includes(URL url) {
                return !JAR_PROTOCOL.equals(url.getProtocol()) && !url.getFile().endsWith(".jar");
            }
        }
    }
}
