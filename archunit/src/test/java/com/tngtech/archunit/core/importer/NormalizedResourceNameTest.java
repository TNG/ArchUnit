package com.tngtech.archunit.core.importer;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class NormalizedResourceNameTest {
    @DataProvider
    public static Object[][] resource_name_starts_with_cases() {
        return $$(
                $("com", "com", true),
                $("com/foo", "com", true),
                $("/com/", "/com", true),
                $("com", "bar", false),
                $("com", "co", false),
                $("co/m", "co", true),
                $("co/m", "co/m", true),
                $("some/longer/path/more", "some/longer/path", true),
                $("some/longer/path/more", "some/longer", true),
                $("some/longer/path/more", "some/longer/p", false)
        );
    }

    @Test
    @UseDataProvider("resource_name_starts_with_cases")
    public void resource_name_starts_with_other_resource_name(
            String resourceName, String startsWith, boolean expectedResult) {

        assertThat(NormalizedResourceName.from(resourceName).startsWith(NormalizedResourceName.from(startsWith)))
                .as(String.format("'%s' startsWith '%s'", resourceName, startsWith))
                .isEqualTo(expectedResult);
    }

    @DataProvider
    public static Object[][] names_to_absolute_names() {
        return $$(
                $("", "/"),
                $("com", "/com/"),
                $("com/foo", "/com/foo/"),
                $("Some.class", "/Some.class"),
                $("com/Some.class", "/com/Some.class")
        );
    }

    @Test
    @UseDataProvider("names_to_absolute_names")
    public void creates_absolute_path(String input, String expectedAbsolutePath) {
        NormalizedResourceName resourceName = NormalizedResourceName.from(input);

        assertThat(resourceName.toAbsolutePath()).isEqualTo(expectedAbsolutePath);
    }

    @DataProvider
    public static Object[][] names_to_entry_names() {
        return $$(
                $("", ""),
                $("/com", "com/"),
                $("/com/foo", "com/foo/"),
                $("Some.class", "Some.class"),
                $("/com/Some.class", "com/Some.class")
        );
    }

    @Test
    @UseDataProvider("names_to_entry_names")
    public void creates_entry_name(String input, String expectedEntryName) {
        NormalizedResourceName resourceName = NormalizedResourceName.from(input);

        assertThat(resourceName.toEntryName()).isEqualTo(expectedEntryName);
    }
}