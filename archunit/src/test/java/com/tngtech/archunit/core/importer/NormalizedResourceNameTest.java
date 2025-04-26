package com.tngtech.archunit.core.importer;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.testutil.DataProviders.$;
import static org.assertj.core.api.Assertions.assertThat;

public class NormalizedResourceNameTest {
    static Stream<Arguments> resource_name_starts_with_cases() {
        return Stream.of(
                $("com", "com", true),
                $("com/foo", "com", true),
                $("/com/", "/com", true),
                $("\\com\\foo", "/com/foo", true),
                $("com", "bar", false),
                $("com", "co", false),
                $("co/m", "co", true),
                $("co/m", "co/m", true),
                $("some/longer/path/more", "some/longer/path", true),
                $("some/longer/path/more", "some/longer", true),
                $("some/longer/path/more", "some/longer/p", false)
        );
    }

    @ParameterizedTest
    @MethodSource("resource_name_starts_with_cases")
    void resource_name_starts_with_other_resource_name(
            String resourceName, String startsWith, boolean expectedResult) {

        assertThat(NormalizedResourceName.from(resourceName).startsWith(NormalizedResourceName.from(startsWith)))
                .as(String.format("'%s' startsWith '%s'", resourceName, startsWith))
                .isEqualTo(expectedResult);
    }

    static Stream<Arguments> names_to_absolute_names() {
        return Stream.of(
                $("", "/"),
                $("com", "/com/"),
                $("com/foo", "/com/foo/"),
                $("Some.class", "/Some.class"),
                $("com/Some.class", "/com/Some.class")
        );
    }

    @ParameterizedTest
    @MethodSource("names_to_absolute_names")
    void creates_absolute_path(String input, String expectedAbsolutePath) {
        NormalizedResourceName resourceName = NormalizedResourceName.from(input);

        assertThat(resourceName.toAbsolutePath()).isEqualTo(expectedAbsolutePath);
    }

    static Stream<Arguments> names_to_entry_names() {
        return Stream.of(
                $("", ""),
                $("/com", "com/"),
                $("/com/foo", "com/foo/"),
                $("Some.class", "Some.class"),
                $("/com/Some.class", "com/Some.class")
        );
    }

    @ParameterizedTest
    @MethodSource("names_to_entry_names")
    void creates_entry_name(String input, String expectedEntryName) {
        NormalizedResourceName resourceName = NormalizedResourceName.from(input);

        assertThat(resourceName.toEntryName()).isEqualTo(expectedEntryName);
    }
}