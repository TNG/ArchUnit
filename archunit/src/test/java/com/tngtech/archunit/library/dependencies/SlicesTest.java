package com.tngtech.archunit.library.dependencies;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.DescribedIterable;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.TestUtils;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.domain.TestUtils.dependencyFrom;
import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.simulateCall;
import static org.assertj.core.api.Assertions.assertThat;

public class SlicesTest {
    @Test
    public void matches_slices() {
        JavaClasses classes = importClassesWithContext(Object.class, String.class, List.class, Set.class, Pattern.class);

        assertThat(Slices.matching("java.(*)..").transform(classes)).hasSize(2);
        assertThat(Slices.matching("(**)").transform(classes)).hasSize(3);
        assertThat(Slices.matching("java.(**)").transform(classes)).hasSize(3);
        assertThat(Slices.matching("java.(*).(*)").transform(classes)).hasSize(1);
    }

    @Test
    public void default_naming_slices() {
        JavaClasses classes = importClassesWithContext(Object.class, String.class, Pattern.class);
        DescribedIterable<Slice> slices = Slices.matching("java.(*)..").transform(classes);

        assertThat(slices).extractingResultOf("getDescription").containsOnly("Slice lang", "Slice util");
    }

    @Test
    public void renaming_slices() {
        JavaClasses classes = importClassesWithContext(Object.class, String.class, Pattern.class);
        DescribedIterable<Slice> slices = Slices.matching("java.(*)..").namingSlices("Hallo $1").transform(classes);

        assertThat(slices).extractingResultOf("getDescription").containsOnly("Hallo lang", "Hallo util");
    }

    @Test
    public void name_parts_are_resolved_correctly() {
        JavaClasses classes = importClassesWithContext(Object.class);
        DescribedIterable<Slice> slices = Slices.matching("(*).(*)..").transform(classes);

        assertThat(getOnlyElement(slices).getNamePart(1)).isEqualTo("java");
        assertThat(getOnlyElement(slices).getNamePart(2)).isEqualTo("lang");
    }

    @Test
    public void slices_of_dependencies() {
        JavaMethod methodThatCallsJavaUtil = TestUtils.importClassWithContext(Object.class).getMethod("toString");
        JavaMethod methodThatCallsJavaLang = TestUtils.importClassWithContext(Map.class).getMethod("put", Object.class, Object.class);
        simulateCall().from(methodThatCallsJavaUtil, 5).to(methodThatCallsJavaLang);
        simulateCall().from(methodThatCallsJavaLang, 1).to(methodThatCallsJavaUtil);

        Dependency first = dependencyFrom(getOnlyElement(methodThatCallsJavaUtil.getMethodCallsFromSelf()));
        Dependency second = dependencyFrom(getOnlyElement(methodThatCallsJavaLang.getMethodCallsFromSelf()));

        Slices slices = Slices.matching("java.(*)..").transform(ImmutableSet.of(first, second));

        assertThat(slices).extractingResultOf("getDescription").containsOnly("Slice lang", "Slice util");
    }
}