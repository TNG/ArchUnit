package com.tngtech.archunit;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;

import com.google.common.collect.ImmutableMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.tngtech.archunit.testutil.ReflectionTestUtils.constructor;
import static org.assertj.core.api.Assertions.assertThat;

public class ArchConfigurationTest {
    private static final String PROPERTIES_FILE_NAME = "archconfigtest.properties";
    private static final String PROPERTIES_RESOURCE_NAME = "/" + PROPERTIES_FILE_NAME;
    private final File testPropsFile = new File(getClass().getResource("/").getFile(), PROPERTIES_FILE_NAME);

    @Before
    public void setUp() {
        testPropsFile.delete();
    }

    @After
    public void tearDown() {
        testPropsFile.delete();
    }

    @Test
    public void missing_property_file() {
        ArchConfiguration configuration = testConfiguration("notThere");

        assertDefault(configuration);
    }

    @Test
    public void empty_property_file() {
        writeProperties(Collections.<String, String>emptyMap());

        assertDefault(testConfiguration(PROPERTIES_RESOURCE_NAME));
    }

    @Test
    public void properties_explicitly_set() {
        writeProperties(ImmutableMap.of(
                ArchConfiguration.RESOLVE_MISSING_DEPENDENCIES_FROM_CLASS_PATH, true
        ));

        ArchConfiguration configuration = testConfiguration(PROPERTIES_RESOURCE_NAME);

        assertThat(configuration.resolveMissingDependenciesFromClassPath()).isTrue();
    }

    @Test
    public void reset_works() {
        ArchConfiguration configuration = testConfiguration(PROPERTIES_RESOURCE_NAME);
        assertThat(configuration.resolveMissingDependenciesFromClassPath()).isFalse();

        configuration.setResolveMissingDependenciesFromClassPath(true);
        assertThat(configuration.resolveMissingDependenciesFromClassPath()).isTrue();

        configuration.reset();
        assertThat(configuration.resolveMissingDependenciesFromClassPath()).isFalse();
    }

    @Test
    public void global_access_to_configuration_has_no_error() {
        ArchConfiguration configuration = ArchConfiguration.get();
        configuration.setResolveMissingDependenciesFromClassPath(true);
        assertThat(configuration.resolveMissingDependenciesFromClassPath()).isTrue();
    }

    private void writeProperties(Map<String, ?> props) {
        Properties save = new Properties();
        for (Map.Entry<String, ?> entry : props.entrySet()) {
            save.setProperty(entry.getKey(), "" + entry.getValue());
        }
        try (FileOutputStream outputStream = new FileOutputStream(testPropsFile)) {
            save.store(outputStream, "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void assertDefault(ArchConfiguration configuration) {
        assertThat(configuration.resolveMissingDependenciesFromClassPath()).isFalse();
    }

    private ArchConfiguration testConfiguration(String resourceName) {
        Constructor<?> constructor = constructor(ArchConfiguration.class, String.class);
        constructor.setAccessible(true);
        try {
            return (ArchConfiguration) constructor.newInstance(resourceName);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}