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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.google.common.base.Preconditions.checkState;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.constructor;
import static com.tngtech.archunit.testutil.TestUtils.properties;
import static com.tngtech.archunit.testutil.TestUtils.singleProperty;
import static org.assertj.core.api.Assertions.entry;

public class ArchConfigurationTest {
    private static final String PROPERTIES_FILE_NAME = "archconfigtest.properties";
    private final File testPropsFile = new File(getClass().getResource("/").getFile(), PROPERTIES_FILE_NAME);

    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        checkState(!testPropsFile.exists() || testPropsFile.delete());
    }

    @After
    public void tearDown() {
        checkState(!testPropsFile.exists() || testPropsFile.delete());
    }

    @Test
    public void missing_property_file() {
        ArchConfiguration configuration = testConfiguration("notThere");

        assertDefault(configuration);
    }

    @Test
    public void empty_property_file() {
        writeProperties(Collections.<String, String>emptyMap());

        assertDefault(testConfiguration(PROPERTIES_FILE_NAME));
    }

    @Test
    public void simple_properties_explicitly_set() {
        writeProperties(ImmutableMap.of(
                ArchConfiguration.RESOLVE_MISSING_DEPENDENCIES_FROM_CLASS_PATH, true,
                ArchConfiguration.ENABLE_MD5_IN_CLASS_SOURCES, true
        ));

        ArchConfiguration configuration = testConfiguration(PROPERTIES_FILE_NAME);

        assertThat(configuration.resolveMissingDependenciesFromClassPath()).isTrue();
        assertThat(configuration.md5InClassSourcesEnabled()).isTrue();
        assertThat(configuration.getClassResolver()).isAbsent();
        assertThat(configuration.getClassResolverArguments()).isEmpty();
    }

    @Test
    public void resolver_explicitly_set() {
        writeProperties(ImmutableMap.of(
                ArchConfiguration.CLASS_RESOLVER, "some.Resolver",
                ArchConfiguration.CLASS_RESOLVER_ARGS, "one.foo,two.bar"
        ));

        ArchConfiguration configuration = testConfiguration(PROPERTIES_FILE_NAME);

        assertThat(configuration.getClassResolver()).contains("some.Resolver");
        assertThat(configuration.getClassResolverArguments()).containsExactly("one.foo", "two.bar");
    }

    @Test
    public void reset_works() {
        ArchConfiguration configuration = testConfiguration(PROPERTIES_FILE_NAME);
        assertThat(configuration.resolveMissingDependenciesFromClassPath()).isTrue();

        configuration.setResolveMissingDependenciesFromClassPath(false);
        assertThat(configuration.resolveMissingDependenciesFromClassPath()).isFalse();

        configuration.reset();
        assertThat(configuration.resolveMissingDependenciesFromClassPath()).isTrue();
    }

    @Test
    public void global_access_to_configuration_has_no_error() {
        ArchConfiguration configuration = ArchConfiguration.get();
        configuration.setResolveMissingDependenciesFromClassPath(true);
        assertThat(configuration.resolveMissingDependenciesFromClassPath()).isTrue();
    }

    @Test
    public void can_set_extension_properties() {
        ArchConfiguration configuration = testConfiguration(PROPERTIES_FILE_NAME);

        configuration.setExtensionProperties("test", singleProperty("key", "value"));

        assertThat(configuration.getExtensionProperties("test")).
                containsOnly(entry("key", "value"));
    }

    @Test
    public void set_extension_properties_are_copied() {
        ArchConfiguration configuration = testConfiguration(PROPERTIES_FILE_NAME);

        Properties properties = singleProperty("key", "value");
        configuration.setExtensionProperties("test", properties);
        properties.setProperty("key", "changed");

        assertThat(configuration.getExtensionProperties("test")).
                containsOnly(entry("key", "value"));
    }

    @Test
    public void can_change_extension_properties() {
        ArchConfiguration configuration = testConfiguration(PROPERTIES_FILE_NAME);

        configuration.setExtensionProperties("test",
                properties("one", "valueOne", "two", "valueTwo"));

        configuration.configureExtension("test")
                .setProperty("two", "changed")
                .setProperty("three", "new");

        assertThat(configuration.getExtensionProperties("test")).containsOnly(
                entry("one", "valueOne"),
                entry("two", "changed"),
                entry("three", "new"));
    }

    @Test
    public void if_no_extension_properties_are_found_empty_properties_are_returned() {
        ArchConfiguration configuration = testConfiguration(PROPERTIES_FILE_NAME);

        assertThat(configuration.getExtensionProperties("not-there")).isEmpty();
    }

    @Test
    public void returned_properties_are_copied() {
        ArchConfiguration configuration = testConfiguration(PROPERTIES_FILE_NAME);

        String original = "value";
        configuration.setExtensionProperties("test", singleProperty("key", original));

        Properties retrievedProps = configuration.getExtensionProperties("test");
        String changed = "changed";
        retrievedProps.setProperty("key", changed);

        assertThat(retrievedProps.getProperty("key")).isEqualTo(changed);
        assertThat(configuration.getExtensionProperties("test").getProperty("key")).isEqualTo(original);
    }

    @Test
    public void creates_extension_properties_from_prefix() {
        writeProperties(ImmutableMap.of(
                "extension.test-extension.enabled", true,
                "extension.test-extension.some-prop", "some value",
                "extension.test-extension.other_prop", 88,
                "extension.other-extension.enabled", false,
                "extension.other-extension.other-prop", "other value"
        ));

        ArchConfiguration configuration = testConfiguration(PROPERTIES_FILE_NAME);

        Properties properties = configuration.getExtensionProperties("test-extension");
        assertThat(properties).containsOnly(
                entry("enabled", "true"), entry("some-prop", "some value"), entry("other_prop", "88"));

        properties = configuration.getExtensionProperties("other-extension");
        assertThat(properties).containsOnly(
                entry("enabled", "false"), entry("other-prop", "other value"));
    }

    @Test
    public void allows_to_specify_custom_properties() {
        writeProperties(ImmutableMap.of(
                "some.custom.booleanproperty", "true",
                "some.custom.stringproperty", "value",
                "toignore", "toignore"
        ));

        ArchConfiguration configuration = testConfiguration(PROPERTIES_FILE_NAME);

        assertThat(configuration.getProperty("some.custom.booleanproperty")).isEqualTo("true");
        assertThat(configuration.getSubProperties("some.custom"))
                .containsExactly(entry("booleanproperty", "true"), entry("stringproperty", "value"));

        configuration.setProperty("some.custom.stringproperty", "changed");
        assertThat(configuration.getSubProperties("some.custom"))
                .containsExactly(entry("booleanproperty", "true"), entry("stringproperty", "changed"));

        assertThat(configuration.containsProperty("not.there"))
                .as("configuration contains property name 'not.there'").isFalse();

        thrown.expect(NullPointerException.class);
        thrown.expectMessage("'not.there'");
        thrown.expectMessage("not configured");
        configuration.getProperty("not.there");
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
        assertThat(configuration.resolveMissingDependenciesFromClassPath()).isTrue();
        assertThat(configuration.md5InClassSourcesEnabled()).isFalse();
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