package com.tngtech.archunit.core.importer;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.io.ByteStreams.toByteArray;
import static com.google.common.primitives.Bytes.asList;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
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
        Location different = Location.of(new TestJarFile().withEntry(fullClassFileName(getClass())).create());

        assertThat(location).isEqualTo(location);
        assertThat(location).isEqualTo(equal);
        assertThat(location.hashCode()).as("HashCode").isEqualTo(equal.hashCode());
        assertThat(location).isNotEqualTo(different);
        assertThat(location).isNotEqualTo(new Object());
    }

    @DataProvider
    public static Object[][] file_locations_pointing_to_jar() throws MalformedURLException {
        JarFile jarFile = new TestJarFile().withEntry(fullClassFileName(LocationTest.class)).create();
        return $$(
                $(Location.of(new URL("file://" + jarFile.getName()))),
                $(Location.of(URI.create("file://" + jarFile.getName()))));
    }

    @Test
    @UseDataProvider("file_locations_pointing_to_jar")
    public void JAR_protocol_is_added_to_file_urls_that_point_to_JARs(Location location) throws MalformedURLException {
        assertThat(location.uri.toString()).startsWith("jar:");
        assertThat(location.uri.toString()).endsWith("!/");
        assertThat(location.uri.toURL().getProtocol()).isEqualTo("jar");
    }

    @Test
    public void File_location_as_ClassFileSource() throws IOException {
        ClassFileSource source = Location.of(urlOfOwnClass()).asClassFileSource(new ImportOptions());

        List<List<Byte>> importedFiles = new ArrayList<>();
        for (ClassFileLocation location : source) {
            try (InputStream s = location.openStream()) {
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
        ClassFileSource source = Location.of(new URL("file://" + jar.getName())).asClassFileSource(new ImportOptions());

        List<List<Byte>> importedFiles = new ArrayList<>();
        for (ClassFileLocation location : source) {
            try (InputStream s = location.openStream()) {
                importedFiles.add(asList(toByteArray(s)));
            }
        }
        assertThat(importedFiles).as("Imported Files as byte arrays")
                .contains(
                        asList(toByteArray(streamOfClass(getClass()))),
                        asList(toByteArray(streamOfClass(Location.class))));
    }

    @DataProvider
    public static Object[][] locations_of_own_class() throws URISyntaxException {
        return $$(
                $(Location.of(urlOfClass(LocationTest.class))),
                $(Location.of(urlOfClass(LocationTest.class).toURI())));
    }

    @Test
    @UseDataProvider("locations_of_own_class")
    public void contains(Location location) {
        assertThat(location.contains("archunit")).as("location contains 'archunit'").isTrue();
        assertThat(location.contains("/archunit/")).as("location contains '/archunit/'").isTrue();
        assertThat(location.contains(getClass().getSimpleName())).as("location contains own simple class name").isTrue();
        assertThat(location.contains("wrong")).as("location contains 'wrong'").isFalse();
    }

    @Test
    public void asUri() throws URISyntaxException {
        URL url = getClass().getResource(".");
        assertThat(Location.of(url).asURI()).isEqualTo(url.toURI());
    }

    @Test
    public void location_of_path() throws URISyntaxException {
        URL url = getClass().getResource(".");
        assertThat(Location.of(Paths.get(url.toURI())).asURI().getPath()).isEqualTo(url.getFile());
    }

    @DataProvider
    public static Object[][] base_locations() {
        return $$(
                $(Location.of(URI.create("file:///some/path"))),
                $(Location.of(URI.create("file:///some/path/"))));
    }

    @Test
    @UseDataProvider("base_locations")
    public void append(Location location) {
        Location appendAbsolute = location.append("/bar/baz");
        Location appendRelative = location.append("bar/baz");

        Location expected = Location.of(Paths.get("/some/path/bar/baz"));
        assertThat(appendAbsolute).isEqualTo(expected);
        assertThat(appendRelative).isEqualTo(expected);
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

    private static InputStream streamOfClass(Class<?> clazz) {
        return clazz.getResourceAsStream(fullClassFileName(clazz));
    }

    private static String fullClassFileName(Class<?> clazz) {
        return String.format("/%s.class", clazz.getName().replace('.', '/'));
    }
}