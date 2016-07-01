package com.tngtech.archunit.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import org.assertj.core.util.Files;

import static com.google.common.io.ByteStreams.toByteArray;

class TestJarFile {
    Set<String> entries = new HashSet<>();

    TestJarFile withEntry(String entry) {
        entries.add(entry);
        return this;
    }

    JarFile create() {
        File folder = Files.newTemporaryFolder();
        File file = new File(folder, "test.jar");

        try (JarOutputStream jarOut = new JarOutputStream(new FileOutputStream(file))) {
            for (String entry : entries) {
                write(jarOut, entry);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return newJarFile(file);
    }

    private void write(JarOutputStream jarOut, String entry) throws IOException {
        jarOut.putNextEntry(new ZipEntry(entry));
        jarOut.write(toByteArray(getClass().getResourceAsStream(entry)));
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
