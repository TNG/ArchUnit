package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import com.tngtech.archunit.testutil.TestUtils;

import static com.google.common.io.ByteStreams.toByteArray;
import static java.nio.file.Files.newOutputStream;
import static java.util.jar.Attributes.Name.MANIFEST_VERSION;
import static org.assertj.core.util.Preconditions.checkArgument;

class TestJarFile {
    private final Manifest manifest;
    private final Set<String> entries = new HashSet<>();

    TestJarFile() {
        manifest = new Manifest();
        manifest.getMainAttributes().put(MANIFEST_VERSION, "1.0");
    }

    TestJarFile withManifestAttribute(Attributes.Name name, String value) {
        manifest.getMainAttributes().put(name, value);
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
        try (JarOutputStream jarOut = new JarOutputStream(newOutputStream(jarFile.toPath()), manifest)) {
            for (String entry : entries) {
                write(jarOut, entry);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return newJarFile(jarFile);
    }

    String createAndReturnName(File jarFile) {
        return createAndReturnName(() -> create(jarFile));
    }

    private void write(JarOutputStream jarOut, String entry) throws IOException {
        checkArgument(!entry.startsWith("/"),
                "ZIP entries must not start with a '/' (compare ZIP spec https://pkware.cachefly.net/webdocs/casestudies/APPNOTE.TXT -> 4.4.17.1)");

        String absoluteResourcePath = "/" + entry;

        ZipEntry zipEntry = new ZipEntry(entry);
        jarOut.putNextEntry(zipEntry);
        if (!zipEntry.isDirectory() && getClass().getResource(absoluteResourcePath) != null) {
            jarOut.write(toByteArray(getClass().getResourceAsStream(absoluteResourcePath)));
        }
        jarOut.closeEntry();
    }

    private JarFile newJarFile(File file) {
        try {
            return new JarFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
