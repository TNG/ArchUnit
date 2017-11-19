package com.tngtech.archunit.core.importer;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import org.assertj.core.api.iterable.Extractor;
import org.junit.Rule;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.importer.LocationTest.urlOfClass;
import static org.assertj.core.api.Assertions.assertThat;

public class LocationsTest {
    @Rule
    public final IndependentClassLoaderRule independentClassLoaderRule = new IndependentClassLoaderRule();

    @Test
    public void locations_of_URLs() throws Exception {
        Collection<Location> locations = Locations.of(ImmutableList.of(
                urlOfClass(getClass()), urlOfClass(Locations.class)));

        assertThat(locations).extracting("uri").containsOnly(
                urlOfClass(getClass()).toURI(), urlOfClass(Locations.class).toURI()
        );
    }

    @Test
    public void locations_of_packages_within_file_URIs() throws Exception {
        Set<Location> locations = Locations.ofPackage("com.tngtech.archunit.core.importer");

        assertThat(locations).extracting("uri").contains(
                uriOfFolderOf(getClass()),
                uriOfFolderOf(Locations.class)
        );
    }

    @Test
    public void locations_of_packages_within_JAR_URIs() throws Exception {
        Set<Location> locations = Locations.ofPackage("org.junit");

        assertThat(locations).extracting("uri").contains(
                uriOfFolderOf(Test.class)
        );
    }

    /**
     * Originally the import of packages had problems, when importing a package where the respective
     * Jar file didn't have an entry for the respective folder (e.g. java.io vs /java/io).
     */
    @Test
    public void locations_of_packages_within_JAR_URIs_that_dont_contain_package_folder() throws Exception {
        independentClassLoaderRule.configureContextClassLoaderAsIndependentClassLoader();

        Set<Location> locations = Locations.ofPackage(independentClassLoaderRule.getIndependentTopLevelPackage());
        ClassFileSource source = getOnlyElement(locations).asClassFileSource(new ImportOptions());

        for (ClassFileLocation classFileLocation : source) {
            try (InputStream ignored = classFileLocation.openStream()) {
                // we only care, that we can open the stream
            }
        }

        assertThat(source)
                .as("URIs in " + independentClassLoaderRule.getIndependentTopLevelPackage())
                .hasSize(independentClassLoaderRule.getNamesOfClasses().size());
    }

    @Test
    public void locations_of_packages_from_mixed_URIs() throws Exception {
        Set<Location> locations = Locations.ofPackage("com.tngtech");

        assertThat(locations).extracting("uri").contains(
                getClass().getResource("/com/tngtech").toURI(),
                resolvedUri(DataProvider.class, "/com/tngtech")
        );
    }

    @Test
    public void locations_of_class_from_file_URI() throws Exception {
        assertThat(Locations.ofClass(getClass())).extracting("uri").containsExactly(
                urlOfClass(getClass()).toURI()
        );
    }

    @Test
    public void locations_of_class_from_JAR_URI() throws Exception {
        assertThat(Locations.ofClass(Test.class)).extracting("uri").containsExactly(
                urlOfClass(Test.class).toURI()
        );
    }

    @Test
    public void locations_in_classpath() throws Exception {
        assertThat(Locations.inClassPath()).extracting("uri").contains(
                getClass().getResource("/").toURI(),
                resolvedUri(DataProvider.class, "/"),
                resolvedUri(Test.class, "/")
        );
    }

    private URI resolvedUri(Class<?> base, String part) throws Exception {
        String urlAsString = urlOfClass(base).toExternalForm();
        String baseResourcePart = '/' + base.getName().replace('.', '/');
        String resolved = urlAsString.substring(0, urlAsString.lastIndexOf(baseResourcePart)) + part;
        return new URL(resolved).toURI();
    }

    private URI uriOfFolderOf(Class<?> clazz) throws Exception {
        String urlAsString = urlOfClass(clazz).toExternalForm();
        return new URL(urlAsString.substring(0, urlAsString.lastIndexOf("/"))).toURI();
    }

    private static Extractor<Location, String> lastUriPart() {
        return new Extractor<Location, String>() {
            @Override
            public String extract(Location input) {
                return input.asURI().toString()
                        .replaceAll("/$", "")
                        .replaceAll(".*/", "");
            }
        };
    }
}