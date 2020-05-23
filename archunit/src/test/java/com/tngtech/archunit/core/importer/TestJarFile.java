package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import com.tngtech.archunit.testutil.TestUtils;

import static com.google.common.io.ByteStreams.toByteArray;
import static java.util.jar.Attributes.Name.MANIFEST_VERSION;

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
        entries.add(entry);
        return this;
    }

    JarFile create() {
        File folder = TestUtils.newTemporaryFolder();
        return create(new File(folder, "test.jar"));
    }

    JarFile create(File jarFile) {
        try (JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(jarFile), manifest)) {
            for (String entry : entries) {
                write(jarOut, entry);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return newJarFile(jarFile);
    }

    private void write(JarOutputStream jarOut, String entry) throws IOException {
        jarOut.putNextEntry(new ZipEntry(entry));
        if (getClass().getResource(entry) != null) {
            jarOut.write(toByteArray(getClass().getResourceAsStream(entry)));
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
