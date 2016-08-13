package com.tngtech.archunit.library.dependencies;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.Dependency;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.core.JavaMethod;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.TestUtils.javaClasses;
import static com.tngtech.archunit.core.TestUtils.javaMethod;
import static com.tngtech.archunit.core.TestUtils.simulateCall;
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

    @Test
    public void name_parts_are_resolved_correctly() {
        JavaClasses classes = javaClasses(Object.class);
        Slices slices = Slices.of(classes).matching("(*).(*)..");

        assertThat(getOnlyElement(slices).getNamePart(1)).isEqualTo("java");
        assertThat(getOnlyElement(slices).getNamePart(2)).isEqualTo("lang");
    }

    @Test
    public void slices_of_dependencies() {
        JavaMethod methodThatCallsJavaUtil = javaMethod(Object.class, "toString");
        JavaMethod methodThatCallsJavaLang = javaMethod(Map.class, "put", Object.class, Object.class);
        simulateCall().from(methodThatCallsJavaUtil, 5).to(methodThatCallsJavaLang);
        simulateCall().from(methodThatCallsJavaLang, 1).to(methodThatCallsJavaUtil);

        Dependency first = Dependency.from(getOnlyElement(methodThatCallsJavaUtil.getMethodCallsFromSelf()));
        Dependency second = Dependency.from(getOnlyElement(methodThatCallsJavaLang.getMethodCallsFromSelf()));

        Slices slices = Slices.matching("java.(*)..").transform(ImmutableSet.of(first, second));

        assertThat(slices).extractingResultOf("getDescription").containsOnly("Slice lang", "Slice util");
    }
}