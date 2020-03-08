package com.tngtech.archunit.core.domain;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;

@RunWith(DataProviderRunner.class)
public class FormattersTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
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
}
