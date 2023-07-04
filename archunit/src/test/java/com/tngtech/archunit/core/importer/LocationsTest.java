package com.tngtech.archunit.core.importer;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.importer.testexamples.SomeEnum;
import com.tngtech.java.junit.dataprovider.DataProvider;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.io.ByteStreams.toByteArray;
import static com.tngtech.archunit.core.importer.LocationTest.classFileEntry;
import static com.tngtech.archunit.core.importer.LocationTest.jarUriOfEntry;
import static com.tngtech.archunit.core.importer.LocationTest.urlOfClass;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;

public class LocationsTest {
    @Rule
    public final IndependentClasspathRule independentClasspathRule = new IndependentClasspathRule();

    @Test
    public void locations_of_URLs() throws Exception {
        Collection<Location> locations = Locations.of(ImmutableList.of(
                urlOfClass(getClass()), urlOfClass(Locations.class)));

        assertThat(urisOf(locations)).containsOnly(
                urlOfClass(getClass()).toURI(), urlOfClass(Locations.class).toURI()
        );
    }

    @Test
    public void locations_of_packages_within_file_URIs() throws Exception {
        Set<Location> locations = Locations.ofPackage("com.tngtech.archunit.core.importer");

        assertThat(urisOf(locations)).contains(
                uriOfFolderOf(getClass()),
                uriOfFolderOf(Locations.class)
        );
    }

    @Test
    public void locations_of_packages_within_JAR_URIs() throws Exception {
        Set<Location> locations = Locations.ofPackage("org.junit");

        assertThat(urisOf(locations)).contains(
                uriOfFolderOf(Test.class)
        );
    }

    /**
     * Originally the import of packages had problems, when importing a package where the respective
     * Jar file didn't have an entry for the respective folder (e.g. java.io vs /java/io).
     */
    @Test
    @SuppressWarnings("EmptyTryBlock")
    public void locations_of_packages_within_JAR_URIs_that_do_not_contain_package_folder() throws Exception {
        independentClasspathRule.configureClasspath();

        Set<Location> locations = Locations.ofPackage(independentClasspathRule.getIndependentTopLevelPackage());
        ClassFileSource source = getOnlyElement(locations).asClassFileSource(new ImportOptions());

        for (ClassFileLocation classFileLocation : source) {
            try (InputStream ignored = classFileLocation.openStream()) {
                // we only care that we can open the stream
            }
        }

        assertThat(source)
                .as("URIs in " + independentClasspathRule.getIndependentTopLevelPackage())
                .hasSize(independentClasspathRule.getNamesOfClasses().size());
    }

    @Test
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public void locations_of_packages_from_custom_ClassLoader_for_JARs_with_directory_entries() throws IOException {
        JarFile jarFile = new TestJarFile()
                .withDirectoryEntries()
                .withEntry(classFileEntry(SomeEnum.class).toAbsolutePath())
                .create();
        URL jarUrl = getJarUrlOf(jarFile);

        Thread.currentThread().setContextClassLoader(new URLClassLoader(new URL[]{jarUrl}, null));

        Location location = Locations.ofPackage(SomeEnum.class.getPackage().getName()).stream()
                .filter(it -> it.contains(jarUrl.toString()))
                .findFirst()
                .get();

        byte[] expectedClassContent = toByteArray(urlOfClass(SomeEnum.class).openStream());
        Stream<byte[]> actualClassContents = stream(location.asClassFileSource(new ImportOptions()))
                .map(it -> unchecked(() -> toByteArray(it.openStream())));

        boolean containsExpectedContent = actualClassContents.anyMatch(it -> Arrays.equals(it, expectedClassContent));
        assertThat(containsExpectedContent)
                .as("one of the actual class files has the expected class file content")
                .isTrue();
    }

    /**
     * This is a known limitation for now: If the JAR file doesn't contain directory entries, then asking
     * the {@link ClassLoader} for all resources within a directory (which happens when we look for a package)
     * will not return anything.
     * For this we have some mitigations to additionally search the classpath, but in case this really is
     * a highly customized {@link ClassLoader} that doesn't expose any URLs there is not much more we can do.
     */
    @Test
    public void locations_of_packages_from_custom_ClassLoader_for_JARs_without_directory_entries() throws IOException {
        JarFile jarFile = new TestJarFile()
                .withoutDirectoryEntries()
                .withEntry(classFileEntry(SomeEnum.class).toAbsolutePath())
                .create();
        URL jarUrl = getJarUrlOf(jarFile);

        Thread.currentThread().setContextClassLoader(new CustomClassLoader(jarUrl));

        String packageName = SomeEnum.class.getPackage().getName();
        assertThat(Locations.ofPackage(packageName))
                .as("Locations of package '%s'", packageName)
                .noneMatch(it -> it.contains(jarUrl.toString()));
    }

    @Test
    public void locations_of_packages_from_mixed_URIs() {
        Set<Location> locations = Locations.ofPackage("com.tngtech");

        assertThat(urisOf(locations)).contains(
                resolvedUri(getClass(), "/com/tngtech"),
                resolvedUri(DataProvider.class, "/com/tngtech")
        );
    }

    @Test
    public void locations_of_class_from_file_URI() throws Exception {
        assertThat(urisOf(Locations.ofClass(getClass()))).containsExactly(
                urlOfClass(getClass()).toURI()
        );
    }

    @Test
    public void locations_of_class_from_JAR_URI() throws Exception {
        assertThat(urisOf(Locations.ofClass(Test.class))).containsExactly(
                urlOfClass(Test.class).toURI()
        );
    }

    @Test
    public void locations_in_classpath() throws Exception {
        assertThat(urisOf(Locations.inClassPath())).contains(
                getClass().getResource("/").toURI(),
                resolvedUri(DataProvider.class, "/"),
                resolvedUri(Test.class, "/")
        );
    }

    private static URL getJarUrlOf(JarFile jarFile) throws MalformedURLException {
        return jarUriOfEntry(jarFile, "").toURL();
    }

    private Iterable<URI> urisOf(Collection<Location> locations) {
        return locations.stream().map(Location::asURI).collect(toSet());
    }

    private URI resolvedUri(Class<?> base, String part) {
        String urlAsString = urlOfClass(base).toExternalForm();
        String baseResourcePart = '/' + base.getName().replace('.', '/');
        String resolved = urlAsString.substring(0, urlAsString.lastIndexOf(baseResourcePart)) + part;
        return NormalizedUri.from(resolved).toURI();
    }

    private URI uriOfFolderOf(Class<?> clazz) throws Exception {
        String urlAsString = urlOfClass(clazz).toExternalForm();
        return new URL(urlAsString.substring(0, urlAsString.lastIndexOf("/")) + "/").toURI();
    }

    private static <T> Stream<T> stream(Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    static <T> T unchecked(ThrowingSupplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @FunctionalInterface
    interface ThrowingSupplier<T> {
        T get() throws Exception;
    }

    private static class CustomClassLoader extends URLClassLoader {
        CustomClassLoader(URL... urls) {
            super(urls, null);
        }

        @Override
        public URL[] getURLs() {
            // Simulate some non-standard ClassLoader by not exposing any URLs we could retrieve from the outside
            return new URL[0];
        }
    }
}
