package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.InitialConfigurationTest;
import com.tngtech.archunit.core.importer.resolvers.ClassResolverFactoryTest;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.assertj.core.api.Condition;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.base.Functions.toStringFunction;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.io.ByteStreams.toByteArray;
import static com.google.common.primitives.Bytes.asList;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class LocationTest {
    @Test
    public void file_resources_are_not_detected_as_JARs() {
        Location location = Location.of(urlOfOwnClass());

        assertThat(location.isJar()).as("Location is a JAR").isFalse();
    }

    @Test
    public void jar_resources_are_detected_as_JARs() {
        JarFile jarFile = new TestJarFile().withEntry(classFileResource(getClass())).create();

        Location location = Location.of(jarFile);

        assertThat(location.isJar()).as("Location is a JAR").isTrue();
    }

    @Test
    public void iterate_entries_of_whole_jar() {
        Set<NormalizedResourceName> entries = ImmutableSet.of(
                classFileEntry(getClass()),
                classFileEntry(ArchConfiguration.class),
                packageEntry(DescribedPredicate.class),
                classFileEntry(DescribedPredicate.class)
        );
        JarFile jarFile = jarFileContaining(entries);

        Location location = Location.of(jarFile);

        assertThat(location.iterateEntries())
                .as("entries of JAR")
                .containsOnlyElementsOf(Sets.difference(entries, singleton(packageEntry(DescribedPredicate.class))));
    }

    @Test
    public void iterate_entries_of_package_of_jar_url() {
        Set<NormalizedResourceName> entries = ImmutableSet.of(
                classFileEntry(getClass()),
                classFileEntry(ArchConfiguration.class),
                packageEntry(DescribedPredicate.class),
                classFileEntry(DescribedPredicate.class)
        );
        JarFile jarFile = jarFileContaining(entries);

        Location location = Location.of(jarUriOfEntry(jarFile, packageEntry(ArchConfiguration.class)));

        assertThat(location.iterateEntries())
                .as("entries of JAR")
                .containsOnlyElementsOf(Sets.difference(entries, singleton(packageEntry(DescribedPredicate.class))));

        location = Location.of(jarUriOfEntry(jarFile, packageEntry(getClass())));

        assertThat(location.iterateEntries())
                .as("entries of JAR")
                .containsOnly(classFileEntry(getClass()));

        location = Location.of(jarUriOfEntry(jarFile, classFileResource(getClass())));

        assertThat(location.iterateEntries())
                .as("entries of JAR")
                .containsOnly(classFileEntry(getClass()));
    }

    @Test
    public void iterate_entries_of_non_existing_jar_url() {
        File nonExistingJar = new File(createNonExistingFolder(), "not-there.jar");

        Location location = Location.of(URI.create("jar:" + nonExistingJar.toURI() + "!/"));

        assertThat(location.iterateEntries())
                .as("entries of JAR")
                .isEmpty();
    }

    @Test
    public void iterate_entries_of_file_url() throws Exception {
        Location location = Location.of(getClass().getResource(packageResource(getClass())));

        assertThat(location.iterateEntries())
                .as("entries of DIR")
                .contains(sameLevel(getClass()))
                .contains(oneLevelBelow(ClassResolverFactoryTest.class));

        assertThat(transform(location.iterateEntries(), toStringFunction()))
                .as("entries of DIR")
                .doNotHave(elementWithSubstring(InitialConfigurationTest.class.getSimpleName()));
    }

    @Test
    public void iterate_entries_of_non_existing_file_url() {
        Location location = Location.of(createNonExistingFolder().toURI());

        assertThat(location.iterateEntries())
                .as("entries of DIR")
                .isEmpty();
    }

    private File createNonExistingFolder() {
        File nonExistingFile = new File("/not/there");
        checkState(!nonExistingFile.exists(), "File should not exist");
        return nonExistingFile;
    }

    private JarFile jarFileContaining(Set<NormalizedResourceName> entries) {
        TestJarFile result = new TestJarFile();
        for (NormalizedResourceName entry : entries) {
            result.withEntry(entry.toString());
        }
        return result.create();
    }

    private NormalizedResourceName sameLevel(Class<?> clazz) {
        return NormalizedResourceName.from(clazz.getSimpleName() + ".class");
    }

    private NormalizedResourceName oneLevelBelow(Class<ClassResolverFactoryTest> clazz) throws URISyntaxException {
        Path absolutePath = absolutePathOf(clazz);
        Path parentFolder = absolutePath.resolve("../..").normalize();
        return NormalizedResourceName.from(parentFolder.relativize(absolutePath).toString());
    }

    private Condition<Object> elementWithSubstring(final String substring) {
        return new Condition<Object>(String.format("element with substring '%s'", substring)) {
            @Override
            public boolean matches(Object value) {
                return value instanceof String && ((String) value).contains(substring);
            }
        };
    }

    private Path absolutePathOf(Class<?> clazz) throws URISyntaxException {
        return new File(urlOfClass(clazz).toURI()).getAbsoluteFile().toPath();
    }

    private URI jarUriOfEntry(JarFile jarFile, String entry) {
        return jarUriOfEntry(jarFile, NormalizedResourceName.from(entry));
    }

    private URI jarUriOfEntry(JarFile jarFile, NormalizedResourceName entry) {
        return URI.create("jar:" + new File(jarFile.getName()).toURI().toString() + "!/" + entry);
    }

    @Test
    public void equals_and_hashcode() {
        JarFile jarFile = new TestJarFile().withEntry(classFileResource(getClass())).create();
        Location location = Location.of(jarFile);
        Location equal = Location.of(jarFile);
        Location different = Location.of(new TestJarFile().withEntry(classFileResource(getClass())).create());

        assertThat(location).isEqualTo(location);
        assertThat(location).isEqualTo(equal);
        assertThat(location.hashCode()).as("HashCode").isEqualTo(equal.hashCode());
        assertThat(location).isNotEqualTo(different);
        assertThat(location).isNotEqualTo(new Object());
    }

    @DataProvider
    public static Object[][] file_locations_pointing_to_jar() throws MalformedURLException {
        JarFile jarFile = new TestJarFile().withEntry(classFileResource(LocationTest.class)).create();
        return $$(
                $(Location.of(new File(jarFile.getName()).toURI().toURL())),
                $(Location.of(new File(jarFile.getName()).toURI())));
    }

    @Test
    @UseDataProvider("file_locations_pointing_to_jar")
    public void JAR_protocol_is_added_to_file_urls_that_point_to_JARs(Location location) throws MalformedURLException {
        assertThat(location.uri.toString()).startsWith("jar:");
        assertThat(location.uri.toString()).endsWith("!/");
        assertThat(location.uri.toURI().toURL().getProtocol()).isEqualTo("jar");
    }

    @Test
    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
    public void JAR_location_as_ClassFileSource() throws IOException {
        JarFile jar = new TestJarFile()
                .withEntry(classFileResource(getClass()))
                .withEntry(classFileResource(Location.class))
                .create();
        ClassFileSource source = Location.of(new File(jar.getName()).toURI()).asClassFileSource(new ImportOptions());

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
    @UseDataProvider("locations_of_own_class")
    public void matches(Location location) {
        assertThat(location.matches(Pattern.compile("archunit.*"))).as("location matches 'archunit.*'").isFalse();
        assertThat(location.matches(Pattern.compile(".*archunit.*"))).as("location matches '.*archunit.*'").isTrue();
        assertThat(location.matches(Pattern.compile(".*/archunit/.*"))).as("location contains '/archunit/'").isTrue();
        assertThat(location.matches(Pattern.compile(".*" + getClass().getSimpleName() + ".*")))
                .as("location matches own simple class name").isTrue();
        assertThat(location.matches(Pattern.compile(".*wrong.*"))).as("location matches '.*wrong.*'").isFalse();
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
                $(Location.of(withoutTrailingSlash(Paths.get("/some/path").toUri()))),
                $(Location.of(withTrailingSlash(Paths.get("/some/path/").toUri()))));
    }

    private static URI withoutTrailingSlash(URI uri) {
        return URI.create(uri.toString().replaceAll("/*$", ""));
    }

    private static URI withTrailingSlash(URI uri) {
        return URI.create(uri.toString().replaceAll("/*$", "/"));
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

    @Test
    public void append_path_with_white_space() {
        JarFile jarFile = new TestJarFile()
                .withEntry("path with spaces")
                .withEntry("path with spaces/like kotlin does.class")
                .create();

        URI dirInJar = Paths.get(jarFile.getName()).toUri();

        Location location = Location.of(dirInJar).append("path with spaces/like kotlin does.class");

        assertThat(location.asURI().toString()).contains("kotlin");
    }

    private URL urlOfOwnClass() {
        return urlOfClass(getClass());
    }

    private InputStream streamOfOwnClass() {
        return streamOfClass(getClass());
    }

    static URL urlOfClass(Class<?> clazz) {
        return clazz.getResource(classFileResource(clazz));
    }

    private static InputStream streamOfClass(Class<?> clazz) {
        return clazz.getResourceAsStream(classFileResource(clazz));
    }

    private static NormalizedResourceName classFileEntry(Class<?> clazz) {
        return NormalizedResourceName.from(classFileResource(clazz));
    }

    private static String classFileResource(Class<?> clazz) {
        return String.format("/%s.class", clazz.getName().replace('.', '/'));
    }

    private NormalizedResourceName packageEntry(Class<?> clazz) {
        return NormalizedResourceName.from(clazz.getPackage().getName().replace('.', '/'));
    }

    private String packageResource(Class<?> clazz) {
        return "/" + packageEntry(clazz);
    }
}