package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;

import com.tngtech.archunit.testutil.TestUtils;

import static com.google.common.io.ByteStreams.toByteArray;
import static java.nio.file.Files.newOutputStream;
import static java.util.jar.Attributes.Name.MANIFEST_VERSION;
import static org.assertj.core.util.Preconditions.checkArgument;

class TestJarFile {
    private final Manifest manifest;
    private Optional<String> nestedClassFilesDirectory = Optional.empty();
    private final Set<String> entries = new HashSet<>();
    private boolean withDirectoryEntries = false;

    TestJarFile() {
        manifest = new Manifest();
        manifest.getMainAttributes().put(MANIFEST_VERSION, "1.0");
    }

    public TestJarFile withDirectoryEntries() {
        withDirectoryEntries = true;
        return this;
    }

    public TestJarFile withoutDirectoryEntries() {
        withDirectoryEntries = false;
        return this;
    }

    TestJarFile withManifestAttribute(Attributes.Name name, String value) {
        manifest.getMainAttributes().put(name, value);
        return this;
    }

    public TestJarFile withNestedClassFilesDirectory(String relativePath) {
        nestedClassFilesDirectory = Optional.of(relativePath);
        return this;
    }

    TestJarFile withEntry(String entry) {
        // ZIP entries must not start with a '/' (compare ZIP spec https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT -> 4.4.17.1)
        entries.add(entry.replaceAll("^/", ""));
        return this;
    }

    JarFile create() {
        File folder = TestUtils.newTemporaryFolder();
        return create(new File(folder, "test.jar"));
    }

    public String createAndReturnName() {
        return createAndReturnName(this::create);
    }

    private String createAndReturnName(Supplier<JarFile> createJarFile) {
        try (JarFile jarFile = createJarFile.get()) {
            return jarFile.getName();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    JarFile create(File jarFile) {
        Stream<TestJarEntry> testJarEntries = entries.stream()
                .map(entry -> new TestJarEntry(entry, nestedClassFilesDirectory));

        Stream<TestJarEntry> allEntries = withDirectoryEntries
                ? ensureDirectoryEntries(testJarEntries)
                : ensureNestedClassFilesDirectoryEntries(testJarEntries);

        try (JarOutputStream jarOut = new JarOutputStream(newOutputStream(jarFile.toPath()), manifest)) {
            allEntries.distinct().forEach(entry -> write(jarOut, entry));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return newJarFile(jarFile);
    }

    private Stream<TestJarEntry> ensureNestedClassFilesDirectoryEntries(Stream<TestJarEntry> entries) {
        return createAdditionalEntries(entries, TestJarEntry::getDirectoriesInPathOfNestedClassFilesDirectory);
    }

    private Stream<TestJarEntry> ensureDirectoryEntries(Stream<TestJarEntry> entries) {
        return createAdditionalEntries(entries, TestJarEntry::getDirectoriesInPath);
    }

    private static Stream<TestJarEntry> createAdditionalEntries(Stream<TestJarEntry> entries, Function<TestJarEntry, Stream<TestJarEntry>> createAdditionalEntries) {
        return entries.flatMap(it -> Stream.concat(createAdditionalEntries.apply(it), Stream.of(it)));
    }

    String createAndReturnName(File jarFile) {
        return createAndReturnName(() -> create(jarFile));
    }

    private void write(JarOutputStream jarOut, TestJarEntry entry) {
        try {
            ZipEntry zipEntry = entry.toZipEntry();
            jarOut.putNextEntry(zipEntry);

            String originResourcePath = "/" + entry.entry;
            if (!zipEntry.isDirectory() && getClass().getResource(originResourcePath) != null) {
                jarOut.write(toByteArray(getClass().getResourceAsStream(originResourcePath)));
            }
            jarOut.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private JarFile newJarFile(File file) {
        try {
            return new JarFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class TestJarEntry {
        private final String entry;
        private final String nestedClassFilesDirectory;

        TestJarEntry(String entry, Optional<String> nestedClassFilesDirectory) {
            this(
                    entry,
                    nestedClassFilesDirectory
                            .map(it -> it.endsWith("/") ? it : it + "/")
                            .orElse("")
            );
        }

        private TestJarEntry(String entry, String nestedClassFilesDirectory) {
            checkArgument(!entry.startsWith("/"),
                    "ZIP entries must not start with a '/' (compare ZIP spec https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT -> 4.4.17.1)");
            checkArgument(!nestedClassFilesDirectory.startsWith("/"),
                    "Nested class files dir must be relative (i.e. not start with a '/')");

            this.entry = entry;
            this.nestedClassFilesDirectory = nestedClassFilesDirectory;
        }

        ZipEntry toZipEntry() {
            return new ZipEntry(nestedClassFilesDirectory + entry);
        }

        Stream<TestJarEntry> getDirectoriesInPath() {
            Stream<TestJarEntry> fromClassEntries = getDirectoriesInPath(entry).stream()
                    .map(it -> new TestJarEntry(it, nestedClassFilesDirectory));
            Stream<TestJarEntry> fromNestedClassFilesDir = getDirectoriesInPathOfNestedClassFilesDirectory();
            return Stream.concat(fromClassEntries, fromNestedClassFilesDir);
        }

        Stream<TestJarEntry> getDirectoriesInPathOfNestedClassFilesDirectory() {
            return getDirectoriesInPath(nestedClassFilesDirectory).stream()
                    .map(it -> new TestJarEntry(it, ""));
        }

        private Set<String> getDirectoriesInPath(String entryPath) {
            Set<String> result = new HashSet<>();
            int checkedUpToIndex = -1;
            do {
                checkedUpToIndex = entryPath.indexOf("/", checkedUpToIndex + 1);
                if (checkedUpToIndex != -1) {
                    result.add(entryPath.substring(0, checkedUpToIndex + 1));
                }
            } while (checkedUpToIndex != -1);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            TestJarEntry that = (TestJarEntry) o;
            return Objects.equals(entry, that.entry)
                   && Objects.equals(nestedClassFilesDirectory, that.nestedClassFilesDirectory);
        }

        @Override
        public int hashCode() {
            return Objects.hash(entry, nestedClassFilesDirectory);
        }
    }
}
