package com.tngtech.archunit.core.domain;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static com.google.common.collect.Sets.union;
import static com.google.common.primitives.Primitives.allPrimitiveTypes;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static java.util.Collections.singleton;

@RunWith(DataProviderRunner.class)
public class FormattersTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    @SuppressWarnings("ConstantConditions")
    public void ensureSimpleName_withNullString() {
        thrown.expect(NullPointerException.class);

        Formatters.ensureSimpleName(null);
    }

    @DataProvider
    public static Object[][] simple_name_test_cases() {
        return $$(
                $("", ""),
                $("Dummy", "Dummy"),
                $("org.example.Dummy", "Dummy"),
                $("org.example.Dummy$123", ""),
                $("org.example.Dummy$NestedClass", "NestedClass"),
                $("org.example.Dummy$NestedClass123", "NestedClass123"),
                $("org.example.Dummy$NestedClass$123", ""),
                $("org.example.Dummy$NestedClass$MoreNestedClass", "MoreNestedClass"),
                $("org.example.Dummy$123LocalClass", "LocalClass"),
                $("org.example.Dummy$Inner$123LocalClass", "LocalClass"),
                $("org.example.Dummy$Inner$123LocalClass123", "LocalClass123"),
                $("Dummy[]", "Dummy[]"),
                $("org.example.Dummy[]", "Dummy[]"),
                $("org.example.Dummy$Inner[][]", "Inner[][]"));
    }

    @Test
    @UseDataProvider("simple_name_test_cases")
    public void ensureSimpleName(String input, String expected) {
        assertThat(Formatters.ensureSimpleName(input)).isEqualTo(expected);
    }

    @DataProvider
    public static List<List<String>> canonical_array_name_test_cases() {
        class SomeClass {
        }

        List<List<String>> testCases = new ArrayList<>();
        testCases.add(ImmutableList.of("", ""));
        testCases.add(ImmutableList.of(SomeClass.class.getSimpleName(), SomeClass.class.getSimpleName()));
        testCases.addAll(generateCanonicalNameTestCases(union(ImmutableSet.of(String.class, SomeClass.class), allRelevantPrimitiveTypes())));
        testCases.add(ImmutableList.of("[[Lorg.example.Some$Inner;", "org.example.Some$Inner[][]"));
        return testCases;
    }

    private static Set<Class<?>> allRelevantPrimitiveTypes() {
        return Sets.difference(allPrimitiveTypes(), singleton(void.class));
    }

    private static List<List<String>> generateCanonicalNameTestCases(Iterable<Class<?>> classes) {
        List<List<String>> result = new ArrayList<>();
        for (Class<?> componentType : classes) {
            result.add(ImmutableList.of(componentType.getName(), componentType.getName()));

            Class<?> oneDim = Array.newInstance(componentType, 0).getClass();
            result.add(ImmutableList.of(oneDim.getName(), componentType.getName() + "[]"));

            Class<?> twoDim = Array.newInstance(oneDim, 0).getClass();
            result.add(ImmutableList.of(twoDim.getName(), componentType.getName() + "[][]"));
        }
        return result;
    }

    @Test
    @UseDataProvider("canonical_array_name_test_cases")
    public void ensureCanonicalArrayTypeName(String input, String expected) {
        assertThat(Formatters.ensureCanonicalArrayTypeName(input)).as("Canonical name of '%s'", input).isEqualTo(expected);
    }
}
