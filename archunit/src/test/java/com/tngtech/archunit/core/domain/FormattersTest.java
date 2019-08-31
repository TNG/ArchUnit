package com.tngtech.archunit.core.domain;

import java.util.ArrayList;

import com.tngtech.archunit.testutil.ArchConfigurationRule;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.domain.Formatters.formatLocation;
import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;

@RunWith(DataProviderRunner.class)
public class FormattersTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Rule
    public final ArchConfigurationRule configuration = new ArchConfigurationRule();

    @Before
    public void setUp() {
        // We need this to create a JavaClass without source, i.e. a stub because the class is missing and cannot be resolved
        configuration.resolveAdditionalDependenciesFromClassPath(false);
    }

    @Test
    public void format_location() {
        JavaClass classWithSource = importClassWithContext(Object.class);

        assertThat(classWithSource.getSource()).as("source").isPresent();
        assertThat(formatLocation(classWithSource, 7)).isEqualTo("(Object.java:7)");

        JavaClass classWithoutSource = getClassWithoutSource();

        assertThat(classWithoutSource.getSource()).as("source").isAbsent();
        assertThat(formatLocation(classWithoutSource, 7)).isEqualTo(String.format("(%s.java:7)", classWithoutSource.getSimpleName()));
    }

    private JavaClass getClassWithoutSource() {
        for (JavaAccess<?> javaAccess : importClassWithContext(SomeClass.class).getAccessesFromSelf()) {
            if (javaAccess.getTargetOwner().isEquivalentTo(ArrayList.class)) {
                return javaAccess.getTargetOwner();
            }
        }
        throw new RuntimeException("Could not create any java class without source");
    }

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

    private static class SomeClass {
        public SomeClass() {
            new ArrayList<>();
        }
    }
}