package com.tngtech.archunit.library.dependencies;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.DescribedIterable;
import com.tngtech.archunit.core.Dependency;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.core.JavaMethod;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.TestUtils.javaClassesViaReflection;
import static com.tngtech.archunit.core.TestUtils.javaMethodViaReflection;
import static com.tngtech.archunit.core.TestUtils.simulateCall;
import static org.assertj.core.api.Assertions.assertThat;

public class SlicesTest {
    @Test
    public void matches_slices() {
        JavaClasses classes = javaClassesViaReflection(Object.class, String.class, List.class, Set.class, Pattern.class);

        assertThat(Slices.matching("java.(*)..").transform(classes)).hasSize(2);
        assertThat(Slices.matching("(**)").transform(classes)).hasSize(3);
        assertThat(Slices.matching("java.(**)").transform(classes)).hasSize(3);
        assertThat(Slices.matching("java.(*).(*)").transform(classes)).hasSize(1);
    }

    @Test
    public void default_naming_slices() {
        JavaClasses classes = javaClassesViaReflection(Object.class, String.class, Pattern.class);
        DescribedIterable<Slice> slices = Slices.matching("java.(*)..").transform(classes);

        assertThat(slices).extractingResultOf("getDescription").containsOnly("Slice lang", "Slice util");
    }

    @Test
    public void renaming_slices() {
        JavaClasses classes = javaClassesViaReflection(Object.class, String.class, Pattern.class);
        DescribedIterable<Slice> slices = Slices.matching("java.(*)..").namingSlices("Hallo $1").transform(classes);

        assertThat(slices).extractingResultOf("getDescription").containsOnly("Hallo lang", "Hallo util");
    }

    @Test
    public void name_parts_are_resolved_correctly() {
        JavaClasses classes = javaClassesViaReflection(Object.class);
        DescribedIterable<Slice> slices = Slices.matching("(*).(*)..").transform(classes);

        assertThat(getOnlyElement(slices).getNamePart(1)).isEqualTo("java");
        assertThat(getOnlyElement(slices).getNamePart(2)).isEqualTo("lang");
    }

    @Test
    public void slices_of_dependencies() {
        JavaMethod methodThatCallsJavaUtil = javaMethodViaReflection(Object.class, "toString");
        JavaMethod methodThatCallsJavaLang = javaMethodViaReflection(Map.class, "put", Object.class, Object.class);
        simulateCall().from(methodThatCallsJavaUtil, 5).to(methodThatCallsJavaLang);
        simulateCall().from(methodThatCallsJavaLang, 1).to(methodThatCallsJavaUtil);

        Dependency first = Dependency.from(getOnlyElement(methodThatCallsJavaUtil.getMethodCallsFromSelf()));
        Dependency second = Dependency.from(getOnlyElement(methodThatCallsJavaLang.getMethodCallsFromSelf()));

        Slices slices = Slices.matching("java.(*)..").transform(ImmutableSet.of(first, second));

        assertThat(slices).extractingResultOf("getDescription").containsOnly("Slice lang", "Slice util");
    }
}