package com.tngtech.archunit;

import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Properties;

import com.tngtech.archunit.testutil.SystemPropertiesRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.google.common.base.Preconditions.checkArgument;
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

    @Rule
    public final SystemPropertiesRule systemPropertiesRule = new SystemPropertiesRule();

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
        writeProperties();

        assertDefault(testConfiguration(PROPERTIES_FILE_NAME));
    }

    @Test
    public void simple_properties_explicitly_set() {
        writeProperties(
                ArchConfiguration.RESOLVE_MISSING_DEPENDENCIES_FROM_CLASS_PATH, true,
                ArchConfiguration.ENABLE_MD5_IN_CLASS_SOURCES, true
        );

        ArchConfiguration configuration = testConfiguration(PROPERTIES_FILE_NAME);

        assertThat(configuration.resolveMissingDependenciesFromClassPath()).isTrue();
        assertThat(configuration.md5InClassSourcesEnabled()).isTrue();
        assertThat(configuration.getClassResolver()).isAbsent();
        assertThat(configuration.getClassResolverArguments()).isEmpty();
    }

    @Test
    public void resolver_explicitly_set() {
        writeProperties(
                ArchConfiguration.CLASS_RESOLVER, "some.Resolver",
                ArchConfiguration.CLASS_RESOLVER_ARGS, "one.foo,two.bar"
        );

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
        writeProperties(
                "extension.test-extension.enabled", true,
                "extension.test-extension.some-prop", "some value",
                "extension.test-extension.other_prop", 88,
                "extension.other-extension.enabled", false,
                "extension.other-extension.other-prop", "other value"
        );

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
        writeProperties(
                "some.custom.booleanproperty", "true",
                "some.custom.stringproperty", "value",
                "toignore", "toignore"
        );

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

    @Test
    public void allows_to_override_any_property_via_system_property() {
        String customPropertyName = "my.custom.property";
        String otherPropertyName = "my.other.property";
        writeProperties(
                ArchConfiguration.ENABLE_MD5_IN_CLASS_SOURCES, false,
                customPropertyName, "original",
                otherPropertyName, "other"
        );

        ArchConfiguration configuration = testConfiguration(PROPERTIES_FILE_NAME);

        assertThat(configuration.md5InClassSourcesEnabled()).as("MD5 sum in class sources enabled").isFalse();
        assertThat(configuration.getProperty(customPropertyName)).as("custom property").isEqualTo("original");

        System.setProperty("archunit." + ArchConfiguration.ENABLE_MD5_IN_CLASS_SOURCES, "true");
        System.setProperty("archunit." + customPropertyName, "changed");

        assertThat(configuration.md5InClassSourcesEnabled()).as("MD5 sum in class sources enabled").isTrue();
        assertThat(configuration.getProperty(customPropertyName)).as("custom property").isEqualTo("changed");
        assertThat(configuration.getSubProperties(subPropertyKeyOf(customPropertyName))).containsExactly(
                entry(subPropertyNameOf(customPropertyName), "changed"),
                entry(subPropertyNameOf(otherPropertyName), "other"));
    }

    private String subPropertyKeyOf(String customPropertyName) {
        return customPropertyName.split("\\.")[0];
    }

    private String subPropertyNameOf(String customPropertyName) {
        return customPropertyName.split("\\.", 2)[1];
    }

    private void writeProperties(Object... props) {
        checkArgument(props.length % 2 == 0, "There are more keys than values inside of %s", Arrays.toString(props));
        Properties properties = new Properties();
        for (int i = 0; i < props.length; i += 2) {
            checkArgument(props[i] instanceof String, "Array entry %s is supposed to be a property name, but is no string", props[i]);
            properties.setProperty((String) props[i], "" + props[i + 1]);
        }
        try (FileOutputStream outputStream = new FileOutputStream(testPropsFile)) {
            properties.store(outputStream, "");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void assertDefault(ArchConfiguration configuration) {
        assertThat(configuration.resolveMissingDependenciesFromClassPath())
                .as("configuration.resolveMissingDependenciesFromClassPath()").isTrue();
        assertThat(configuration.md5InClassSourcesEnabled())
                .as("configuration.md5InClassSourcesEnabled()").isFalse();
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
