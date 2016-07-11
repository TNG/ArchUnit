package com.tngtech.archunit.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

import com.google.common.base.Supplier;
import org.junit.Test;

import static com.google.common.io.ByteStreams.toByteArray;
import static com.google.common.primitives.Bytes.asList;
import static org.assertj.core.api.Assertions.assertThat;

public class LocationTest {
    @Test
    public void file_resources_are_not_detected_as_JARs() throws Exception {
        Location location = Location.of(urlOfOwnClass());

        assertThat(location.isJar()).as("Location is a JAR").isFalse();
    }

    @Test
    public void jar_resources_are_detected_as_JARs() throws Exception {
        JarFile jarFile = new TestJarFile().withEntry(fullClassFileName(getClass())).create();

        Location location = Location.of(jarFile);

        assertThat(location.isJar()).as("Location is a JAR").isTrue();
    }

    @Test
    public void equals_and_hashcode() {
        JarFile jarFile = new TestJarFile().withEntry(fullClassFileName(getClass())).create();
        Location location = Location.of(jarFile);
        Location equal = Location.of(jarFile);
        Location different = Location.of(new TestJarFile().withEntry(fullClassFileName(Location.class)).create());

        assertThat(location).isEqualTo(location);
        assertThat(location).isEqualTo(equal);
        assertThat(location.hashCode()).as("HashCode").isEqualTo(equal.hashCode());
        assertThat(location).isNotEqualTo(different);
        assertThat(location).isNotEqualTo(new Object());
    }

    @Test
    public void JAR_protocol_is_added_to_file_urls_that_point_to_JARs() throws MalformedURLException {
        JarFile jarFile = new TestJarFile().withEntry(fullClassFileName(getClass())).create();

        Location location = Location.of(new URL("file://" + jarFile.getName()));

        assertThat(location.uri.toString()).startsWith("jar:");
        assertThat(location.uri.toString()).endsWith("!/");
        assertThat(location.uri.toURL().getProtocol()).isEqualTo("jar");
    }

    @Test
    public void File_location_as_ClassFileSource() throws IOException {
        ClassFileSource source = Location.of(urlOfOwnClass()).asClassFileSource();

        List<List<Byte>> importedFiles = new ArrayList<>();
        for (Supplier<InputStream> stream : source) {
            try (InputStream s = stream.get()) {
                importedFiles.add(asList(toByteArray(s)));
            }
        }
        assertThat(importedFiles).as("Imported Files as byte arrays")
                .contains(asList(toByteArray(streamOfOwnClass())));
    }

    @Test
    public void JAR_location_as_ClassFileSource() throws IOException {
        JarFile jar = new TestJarFile()
                .withEntry(fullClassFileName(getClass()))
                .withEntry(fullClassFileName(Location.class))
                .create();
        ClassFileSource source = Location.of(new URL("file://" + jar.getName())).asClassFileSource();

        List<List<Byte>> importedFiles = new ArrayList<>();
        for (Supplier<InputStream> stream : source) {
            try (InputStream s = stream.get()) {
                importedFiles.add(asList(toByteArray(s)));
            }
        }
        assertThat(importedFiles).as("Imported Files as byte arrays")
                .contains(
                        asList(toByteArray(streamOfClass(getClass()))),
                        asList(toByteArray(streamOfClass(Location.class))));
    }

    @Test
    public void contains_works() {
        Location location = Location.of(urlOfOwnClass());

        assertThat(location.contains("archunit")).as("location contains 'archunit'").isTrue();
        assertThat(location.contains("/archunit/")).as("location contains '/archunit/'").isTrue();
        assertThat(location.contains(getClass().getSimpleName())).as("location contains own simple class name").isTrue();
        assertThat(location.contains("wrong")).as("location contains 'wrong'").isFalse();
    }

    private URL urlOfOwnClass() {
        return urlOfClass(getClass());
    }

    private InputStream streamOfOwnClass() {
        return streamOfClass(getClass());
    }

    static URL urlOfClass(Class<?> clazz) {
        return clazz.getResource(fullClassFileName(clazz));
    }

    static InputStream streamOfClass(Class<?> clazz) {
        return clazz.getResourceAsStream(fullClassFileName(clazz));
    }

    static String fullClassFileName(Class<?> clazz) {
        return String.format("/%s.class", clazz.getName().replace('.', '/'));
    }
}