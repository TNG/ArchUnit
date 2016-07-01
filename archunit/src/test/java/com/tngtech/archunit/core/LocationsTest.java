package com.tngtech.archunit.core;

import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.tngtech.java.junit.dataprovider.DataProvider;
import org.junit.Test;

import static com.tngtech.archunit.core.LocationTest.urlOfClass;
import static org.assertj.core.api.Assertions.assertThat;

public class LocationsTest {
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
        Set<Location> locations = Locations.ofPackage("com.tngtech.archunit.core");

        assertThat(locations).extracting("uri").containsOnly(
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
}