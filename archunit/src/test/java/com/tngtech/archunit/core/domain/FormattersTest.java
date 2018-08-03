package com.tngtech.archunit.core.domain;

import java.util.ArrayList;

import com.tngtech.archunit.testutil.ArchConfigurationRule;
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

    private static class SomeClass {
        public SomeClass() {
            new ArrayList<>();
        }
    }
}