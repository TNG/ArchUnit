package com.tngtech.archunit.library.dependencies;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import com.tngtech.archunit.core.JavaClasses;
import org.junit.Test;

import static com.tngtech.archunit.core.TestUtils.javaClasses;
import static org.assertj.core.api.Assertions.assertThat;

public class SlicesTest {
    @Test
    public void matches_slices() {
        JavaClasses classes = javaClasses(Object.class, String.class, List.class, Set.class, Pattern.class);

        assertThat(Slices.of(classes).matching("java.(*)..")).hasSize(2);
        assertThat(Slices.of(classes).matching("(**)")).hasSize(3);
        assertThat(Slices.of(classes).matching("java.(**)")).hasSize(3);
        assertThat(Slices.of(classes).matching("java.(*).(*)")).hasSize(1);
    }

    @Test
    public void default_naming_slices() {
        JavaClasses classes = javaClasses(Object.class, String.class, Pattern.class);
        Slices slices = Slices.of(classes).matching("java.(*)..");

        assertThat(slices).extractingResultOf("getDescription").containsOnly("Slice lang", "Slice util");
    }

    @Test
    public void renaming_slices() {
        JavaClasses classes = javaClasses(Object.class, String.class, Pattern.class);
        Slices slices = Slices.of(classes).matching("java.(*)..").namingSlices("Hallo $1");

        assertThat(slices).extractingResultOf("getDescription").containsOnly("Hallo lang", "Hallo util");
    }
}