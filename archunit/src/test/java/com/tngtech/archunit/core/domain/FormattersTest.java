package com.tngtech.archunit.core.domain;

import java.util.ArrayList;

import com.tngtech.archunit.testutil.ArchConfigurationRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.Formatters.formatLocation;
import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class FormattersTest {
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
        try {
            Formatters.ensureSimpleName(null);
            Assert.fail("NullPointerException expected, but not thrown");
        } catch (NullPointerException expected) {
        }
    }

    @Test
    public void ensureSimpleName_withEmptyString() {
        assertThat(Formatters.ensureSimpleName("")).isEqualTo("");
    }

    @Test
    public void ensureSimpleName_withClassInDefaultPackage() {
        assertThat(Formatters.ensureSimpleName("Dummy")).isEqualTo("Dummy");
    }

    @Test
    public void ensureSimpleName_withClassInPackage() {
        assertThat(Formatters.ensureSimpleName("org.example.Dummy")).isEqualTo("Dummy");
    }

    @Test
    public void ensureSimpleName_withAnonymousClass() {
        assertThat(Formatters.ensureSimpleName("org.example.Dummy$123")).isEqualTo("");
    }

    @Test
    public void ensureSimpleName_withNestedClass() {
        assertThat(Formatters.ensureSimpleName("org.example.Dummy$NestedClass")).isEqualTo("NestedClass");
    }

    @Test
    public void ensureSimpleName_withAnonymousClassInNestedClass() {
        assertThat(Formatters.ensureSimpleName("org.example.Dummy$NestedClass$123")).isEqualTo("");
    }

    @Test
    public void ensureSimpleName_withDeeplyNestedClass() {
        assertThat(Formatters.ensureSimpleName("org.example.Dummy$NestedClass$MoreNestedClass")).isEqualTo("MoreNestedClass");
    }

    private static class SomeClass {
        public SomeClass() {
            new ArrayList<>();
        }
    }
}