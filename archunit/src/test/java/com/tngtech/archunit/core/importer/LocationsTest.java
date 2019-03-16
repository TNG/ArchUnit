package com.tngtech.archunit.core.importer;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.importer.LocationTest.urlOfClass;
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

    private Iterable<URI> urisOf(Collection<Location> locations) {
        Set<URI> result = new HashSet<>();
        for (Location location : locations) {
            result.add(location.asURI());
        }
        return result;
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
}