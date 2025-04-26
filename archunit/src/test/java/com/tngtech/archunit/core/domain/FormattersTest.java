package com.tngtech.archunit.core.domain;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import static com.google.common.collect.ImmutableList.of;
import static com.google.common.collect.Sets.union;
import static com.google.common.primitives.Primitives.allPrimitiveTypes;
import static com.tngtech.archunit.core.domain.Formatters.joinSingleQuoted;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.DataProviders.$;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FormattersTest {

    @Test
    public void formatNamesOf() {
        assertThat(Formatters.formatNamesOf()).isEmpty();

        assertThat(Formatters.formatNamesOf(List.class, Iterable.class, String.class))
                .containsExactly(List.class.getName(), Iterable.class.getName(), String.class.getName());

        assertThat(Formatters.formatNamesOf(emptyList())).isEmpty();

        assertThat(Formatters.formatNamesOf(of(List.class, Iterable.class, String.class)))
                .containsExactly(List.class.getName(), Iterable.class.getName(), String.class.getName());

        // special case because the inferred type of the list is List<Class<? extends Serializable>>
        assertThat(Formatters.formatNamesOf(of(String.class, Serializable.class)))
                .containsExactly(String.class.getName(), Serializable.class.getName());
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    public void ensureSimpleName_withNullString() {
        assertThatThrownBy(
                () -> Formatters.ensureSimpleName(null)
        ).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @CsvSource(delimiter = ',', ignoreLeadingAndTrailingWhitespace = true, value = {
            "'' , ''",
            "Dummy , Dummy",
            "org.example.Dummy , Dummy",
            "org.example.Dummy$123 , ''",
            "org.example.Dummy$NestedClass , NestedClass",
            "org.example.Dummy$NestedClass123 , NestedClass123",
            "org.example.Dummy$NestedClass$123 , ''",
            "org.example.Dummy$NestedClass$MoreNestedClass , MoreNestedClass",
            "org.example.Dummy$123LocalClass , LocalClass",
            "org.example.Dummy$Inner$123LocalClass , LocalClass",
            "org.example.Dummy$Inner$123LocalClass123 , LocalClass123",
            "Dummy[] , Dummy[]",
            "org.example.Dummy[] , Dummy[]",
            "org.example.Dummy$Inner[][] , Inner[][]"
    })
    void ensureSimpleName(String input, String expected) {
        assertThat(Formatters.ensureSimpleName(input)).isEqualTo(expected);
    }

    static List<Arguments> canonical_array_name_test_cases() {
        class SomeClass {
        }

        return ImmutableList.<Arguments>builder()
                .add($("", ""))
                .add($(SomeClass.class.getSimpleName(), SomeClass.class.getSimpleName()))
                .addAll(generateCanonicalNameTestCases(union(ImmutableSet.of(String.class, SomeClass.class), allRelevantPrimitiveTypes())))
                .add($("[[Lorg.example.Some$Inner;", "org.example.Some$Inner[][]"))
                .build();
    }

    private static Set<Class<?>> allRelevantPrimitiveTypes() {
        return Sets.difference(allPrimitiveTypes(), singleton(void.class));
    }

    private static List<Arguments> generateCanonicalNameTestCases(Iterable<Class<?>> classes) {
        List<Arguments> result = new ArrayList<>();
        for (Class<?> componentType : classes) {
            result.add($(componentType.getName(), componentType.getName()));

            Class<?> oneDim = Array.newInstance(componentType, 0).getClass();
            result.add($(oneDim.getName(), componentType.getName() + "[]"));

            Class<?> twoDim = Array.newInstance(oneDim, 0).getClass();
            result.add($(twoDim.getName(), componentType.getName() + "[][]"));
        }
        return result;
    }

    @ParameterizedTest
    @MethodSource("canonical_array_name_test_cases")
    void ensureCanonicalArrayTypeName(String input, String expected) {
        assertThat(Formatters.ensureCanonicalArrayTypeName(input)).as("Canonical name of '%s'", input).isEqualTo(expected);
    }

    static Stream<Arguments> test_joinSingleQuoted() {
        return Stream.of(
                $(new String[0], ""),
                $(new String[]{"one"}, "'one'"),
                $(new String[]{"one", "two"}, "'one', 'two'"),
                $(new String[]{"one", "two", "three"}, "'one', 'two', 'three'"),
                $(new String[]{"", ""}, "'', ''")
        );
    }

    @ParameterizedTest
    @MethodSource
    void test_joinSingleQuoted(String[] input, String expectedOutput) {
        assertThat(joinSingleQuoted(input)).isEqualTo(expectedOutput);
        assertThat(joinSingleQuoted(stream(input).collect(toList()))).isEqualTo(expectedOutput);
    }
}
