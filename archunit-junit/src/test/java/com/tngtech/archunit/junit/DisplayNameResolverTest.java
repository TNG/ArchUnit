package com.tngtech.archunit.junit;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.testutil.ArchConfigurationRule;
import org.junit.Rule;
import org.junit.Test;

import static com.tngtech.archunit.junit.DisplayNameResolver.JUNIT_DISPLAYNAME_REPLACE_UNDERSCORES_BY_SPACES_PROPERTY_NAME;
import static org.assertj.core.api.Assertions.assertThat;

public class DisplayNameResolverTest {

    @Rule
    public final ArchConfigurationRule archConfigurationRule = new ArchConfigurationRule();

    @Test
    public void replaces_underscores_with_blanks_if_property_is_set_to_true() {
        // Given
        String elementName = "some_element_Name";
        ArchConfiguration.get().setProperty(JUNIT_DISPLAYNAME_REPLACE_UNDERSCORES_BY_SPACES_PROPERTY_NAME, "true");

        // When
        String displayName = DisplayNameResolver.determineDisplayName(elementName);

        // Then
        assertThat(displayName).isEqualTo("some element Name");
    }

    @Test
    public void returns_original_name_if_property_is_set_to_false() {
        // Given
        String elementName = "some_element_Name";
        ArchConfiguration.get().setProperty(JUNIT_DISPLAYNAME_REPLACE_UNDERSCORES_BY_SPACES_PROPERTY_NAME, "false");

        // When
        String displayName = DisplayNameResolver.determineDisplayName(elementName);

        // Then
        assertThat(displayName).isEqualTo("some_element_Name");
    }

    @Test
    public void returns_original_name_if_property_is_unset() {
        // Given
        String elementName = "some_element_Name";
        ArchConfiguration.get().reset();

        // When
        String displayName = DisplayNameResolver.determineDisplayName(elementName);

        // Then
        assertThat(displayName).isEqualTo("some_element_Name");
    }
}