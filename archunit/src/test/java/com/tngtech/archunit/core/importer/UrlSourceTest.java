package com.tngtech.archunit.core.importer;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.testutil.SystemPropertiesRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Sets.union;
import static java.util.jar.Attributes.Name.CLASS_PATH;
import static org.assertj.core.api.Assertions.assertThat;

public class UrlSourceTest {
    static final String JAVA_CLASS_PATH_PROP = "java.class.path";
    private static final String JAVA_BOOT_PATH_PROP = "sun.boot.class.path";

    private static final char CHARACTER_THAT_IS_HOPEFULLY_ILLEGAL_ON_EVERY_PLATFORM = '\0';

    @Rule
    public final SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void resolves_from_system_property() throws MalformedURLException {
        Path firstFileEntry = Paths.get("some", "path", "classes");
        Path firstJarEntry = Paths.get("other", "lib", "some.jar");
        Path secondFileEntry = Paths.get("more", "classes");
        Path secondJarEntry = Paths.get("my", ".m2", "repo", "greatlib.jar");
        String classPath = createClassPathProperty(firstFileEntry.toString(), firstJarEntry.toString(),
                secondFileEntry.toString(), secondJarEntry.toString());
        System.setProperty(JAVA_CLASS_PATH_PROP, classPath);

        Path bootstrapFileEntry = Paths.get("some", "bootstrap", "classes");
        Path bootstrapJarEntry = Paths.get("more", "bootstrap", "bootlib.jar");
        String bootstrapClassPath = createClassPathProperty(bootstrapFileEntry.toString(), bootstrapJarEntry.toString());
        System.setProperty(JAVA_BOOT_PATH_PROP, bootstrapClassPath);

        UrlSource urlSource = UrlSource.From.classPathSystemProperties();

        assertThat(urlSource).containsOnly(
                toUrl(firstFileEntry),
                new URL("jar:" + firstJarEntry.toUri() + "!/"),
                toUrl(secondFileEntry),
                new URL("jar:" + secondJarEntry.toUri() + "!/"),
                toUrl(bootstrapFileEntry),
                new URL("jar:" + bootstrapJarEntry.toUri() + "!/")
        );
    }

    @Test
    public void resolves_missing_system_properties_resiliently() {
        System.clearProperty(JAVA_BOOT_PATH_PROP);
        System.clearProperty(JAVA_CLASS_PATH_PROP);

        assertThat(UrlSource.From.classPathSystemProperties()).isEmpty();
    }

    @Test
    public void ignores_invalid_paths_in_class_path_property() {
        Path valid = Paths.get("some", "valid", "path");

        String classPath = createClassPathProperty(valid.toString(),
                "/invalid/path/because/of/" + CHARACTER_THAT_IS_HOPEFULLY_ILLEGAL_ON_EVERY_PLATFORM + "/");
        System.setProperty(JAVA_CLASS_PATH_PROP, classPath);
        System.clearProperty(JAVA_BOOT_PATH_PROP);

        assertThat(UrlSource.From.classPathSystemProperties()).containsOnly(toUrl(valid));
    }

    @Test
    public void returns_unique_urls() {
        URL url = getClass().getResource(".");
        ImmutableList<URL> redundantInput = ImmutableList.of(url, url);

        UrlSource source = UrlSource.From.iterable(redundantInput);

        assertThat(source).hasSize(1).containsOnly(url);
    }

    @Test
    public void handles_paths_with_spaces() throws Exception {
        Path path_with_spaces = temporaryFolder.newFolder("path with spaces").toPath();
        Path destination = path_with_spaces.resolve(getClass().getName() + ".class");
        Files.copy(Paths.get(LocationTest.urlOfClass(getClass()).toURI()), destination);

        String classPath = createClassPathProperty(destination.toString());
        System.setProperty(JAVA_CLASS_PATH_PROP, classPath);
        UrlSource urls = UrlSource.From.classPathSystemProperties();

        assertThat(urls).contains(toUrl(destination));
    }

    @Test
    public void recursively_resolves_classpath_attributes_in_manifests() throws Exception {
        File folder = temporaryFolder.newFolder();
        WrittenJarFile grandChildOne = writeJarWithManifestClasspathAttribute(folder, subPath("grandchild", "one"));
        WrittenJarFile grandChildTwo = writeJarWithManifestClasspathAttribute(folder, subPath("grandchild", "two"));
        WrittenJarFile grandChildThree = writeJarWithManifestClasspathAttribute(folder, subPath("grandchild", "three"));
        WrittenJarFile childOne = writeJarWithManifestClasspathAttribute(folder, subPath("child", "one"), grandChildOne.getPathAsAbsoluteUrl(), ManifestClasspathEntry.relativeUrl(grandChildTwo.path));
        WrittenJarFile childTwo = writeJarWithManifestClasspathAttribute(folder, subPath("child", "two"), ManifestClasspathEntry.absoluteUrl(grandChildThree.path));
        WrittenJarFile parent = writeJarWithManifestClasspathAttribute(folder, "parent", ManifestClasspathEntry.relativePath(childOne.path), ManifestClasspathEntry.absoluteUrl(childTwo.path));

        System.setProperty(JAVA_CLASS_PATH_PROP, parent.path.toString());
        UrlSource urls = UrlSource.From.classPathSystemProperties();

        assertThat(urls).containsAll(concat(
                grandChildOne.getExpectedClasspathUrls(),
                grandChildTwo.getExpectedClasspathUrls(),
                grandChildThree.getExpectedClasspathUrls(),
                childOne.getExpectedClasspathUrls(),
                childTwo.getExpectedClasspathUrls(),
                parent.getExpectedClasspathUrls()));
    }

    @Test
    public void terminates_recursively_resolving_manifest_classpaths_if_manifests_have_circular_reference() throws Exception {
        File folder = temporaryFolder.newFolder();
        File jarOnePath = new File(folder, "one.jar");
        File jarTwoPath = new File(folder, "two.jar");
        JarFile jarOne = new TestJarFile()
                .withManifestAttribute(CLASS_PATH, jarTwoPath.getAbsolutePath())
                .create(jarOnePath);
        JarFile jarTwo = new TestJarFile()
                .withManifestAttribute(CLASS_PATH, jarOnePath.getAbsolutePath())
                .create(jarTwoPath);

        System.setProperty(JAVA_CLASS_PATH_PROP, jarOne.getName());
        System.clearProperty(JAVA_BOOT_PATH_PROP);
        UrlSource urls = UrlSource.From.classPathSystemProperties();

        assertThat(urls).containsOnly(toUrl(Paths.get(jarOne.getName())), toUrl(Paths.get(jarTwo.getName())));
    }

    private String subPath(String... parts) {
        return Joiner.on(File.separator).join(parts);
    }

    private WrittenJarFile writeJarWithManifestClasspathAttribute(final File folder, String identifier, ManifestClasspathEntry... additionalClasspathManifestClasspathEntries) {
        Set<ManifestClasspathEntry> classpathManifestEntries = union(createManifestClasspathEntries(identifier), ImmutableSet.copyOf(additionalClasspathManifestClasspathEntries));
        JarFile jarFile = new TestJarFile()
                .withManifestAttribute(CLASS_PATH, Joiner.on(" ").join(FluentIterable.from(classpathManifestEntries).transform(resolveTo(folder)).toSet()))
                .create(new File(folder, identifier.replace(File.separator, "-") + ".jar"));
        return new WrittenJarFile(Paths.get(jarFile.getName()), classpathManifestEntries);
    }

    private Function<ManifestClasspathEntry, String> resolveTo(final File folder) {
        return new Function<ManifestClasspathEntry, String>() {
            @Override
            public String apply(ManifestClasspathEntry manifestClasspathEntry) {
                return manifestClasspathEntry.create(folder);
            }
        };
    }

    private Set<ManifestClasspathEntry> createManifestClasspathEntries(String infix) {
        Set<ManifestClasspathEntry> result = new HashSet<>();
        for (int i = 0; i < 10; i++) {
            result.add(ManifestClasspathEntry
                    .absolutePath(Paths.get(File.separator + subPath("some", "path", "parent", infix + i, "")).toAbsolutePath()));
        }
        return result;
    }

    private String createClassPathProperty(String... paths) {
        return Joiner.on(File.pathSeparatorChar).join(paths);
    }

    private static class WrittenJarFile {
        private final Path path;
        private final Set<ManifestClasspathEntry> classpathManifestEntries;

        private WrittenJarFile(Path path, Set<ManifestClasspathEntry> classpathManifestEntries) {
            this.path = path;
            this.classpathManifestEntries = classpathManifestEntries;
        }

        public ManifestClasspathEntry getPathAsAbsoluteUrl() {
            return ManifestClasspathEntry.absoluteUrl(path);
        }

        public Iterable<URL> getExpectedClasspathUrls() {
            return FluentIterable.from(classpathManifestEntries)
                    .transform(new Function<ManifestClasspathEntry, URL>() {
                        @Override
                        public URL apply(ManifestClasspathEntry input) {
                            return input.toExpectedClasspathUrl();
                        }
                    });
        }
    }

    private abstract static class ManifestClasspathEntry {
        final Path path;

        protected ManifestClasspathEntry(Path path) {
            this.path = path;
        }

        abstract String create(File folder);

        static ManifestClasspathEntry absoluteUrl(Path path) {
            return new AbsoluteUrl(path);
        }

        static ManifestClasspathEntry absolutePath(Path path) {
            return new AbsolutePath(path);
        }

        static ManifestClasspathEntry relativeUrl(Path path) {
            return new RelativeUrl(path);
        }

        static ManifestClasspathEntry relativePath(Path path) {
            return new RelativePath(path);
        }

        public abstract URL toExpectedClasspathUrl();

        private static class AbsoluteUrl extends ManifestClasspathEntry {
            private AbsoluteUrl(Path path) {
                super(path);
                checkArgument(path.isAbsolute(), "Path is not absolute: %s", path);
            }

            @Override
            String create(File folder) {
                return ensureTrailingSeparatorForFolders(path.toUri().toString());
            }

            @Override
            public URL toExpectedClasspathUrl() {
                return toUrl(path);
            }
        }

        private static class AbsolutePath extends ManifestClasspathEntry {
            private AbsolutePath(Path path) {
                super(path);
                checkArgument(path.isAbsolute(), "Path is not absolute: %s", path);
            }

            @Override
            String create(File folder) {
                String pathString = path.toString();
                return ensureTrailingSeparatorForFolders(pathString);
            }

            @Override
            public URL toExpectedClasspathUrl() {
                return toUrl(path);
            }
        }

        private static class RelativeUrl extends ManifestClasspathEntry {
            private URL expectedClasspathUrl;

            private RelativeUrl(Path path) {
                super(path);
            }

            @Override
            String create(File folder) {
                String relativePath = new RelativePath(path).create(folder);
                this.expectedClasspathUrl = toUrl(folder.toPath().resolve(relativePath));
                return "file:" + relativePath;
            }

            @Override
            public URL toExpectedClasspathUrl() {
                return expectedClasspathUrl;
            }
        }

        private static class RelativePath extends ManifestClasspathEntry {
            private URL expectedClasspathUrl;

            private RelativePath(Path path) {
                super(path);
            }

            @Override
            String create(File folder) {
                Path parent = folder.toPath();
                Path relativePath = parent.relativize(path);
                this.expectedClasspathUrl = toUrl(parent.resolve(relativePath));
                return ensureTrailingSeparatorForFolders(relativePath.toString());
            }

            @Override
            public URL toExpectedClasspathUrl() {
                return expectedClasspathUrl;
            }
        }
    }

    private static String ensureTrailingSeparatorForFolders(String pathString) {
        boolean isFolder = !pathString.matches(".*\\.\\w+$");
        return isFolder
                ? ensureTrailingSeparator(pathString)
                : pathString;
    }

    private static String ensureTrailingSeparator(String pathString) {
        return pathString.endsWith(File.separator) ? pathString : pathString + File.separator;
    }

    private static URL toUrl(Path path) {
        try {
            URL result = path.toUri().toURL();
            return result.toString().endsWith(".jar") ? new URL("jar:" + result + "!/") : result;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
