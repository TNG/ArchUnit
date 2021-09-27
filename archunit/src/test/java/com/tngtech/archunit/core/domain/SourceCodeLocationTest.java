package com.tngtech.archunit.core.domain;

import java.util.ArrayList;

import com.tngtech.archunit.testutil.ArchConfigurationRule;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class SourceCodeLocationTest {

    // We need this to create a JavaClass without source, i.e. a stub because the class is missing and cannot be resolved
    @Rule
    public final ArchConfigurationRule configuration = new ArchConfigurationRule().resolveAdditionalDependenciesFromClassPath(false);

    @Test
    public void format_location() {
        JavaClass classWithSource = importClassWithContext(Object.class);

        assertThat(classWithSource.getSource()).as("source").isPresent();
        Assertions.assertThat(SourceCodeLocation.of(classWithSource, 7).toString()).isEqualTo("(Object.java:7)");

        JavaClass classWithoutSource = getClassWithoutSource();

        assertThat(classWithoutSource.getSource()).as("source").isAbsent();
        Assertions.assertThat(SourceCodeLocation.of(classWithoutSource, 7).toString()).isEqualTo(String.format("(%s.java:7)", classWithoutSource.getSimpleName()));
    }

    @Test
    public void details_of_source_code_location() {
        JavaClass classWithSource = importClassWithContext(Object.class);

        SourceCodeLocation sourceCodeLocation = SourceCodeLocation.of(classWithSource, 7);

        assertThat(sourceCodeLocation.getSourceClass()).as("source class").isEqualTo(classWithSource);
        assertThat(sourceCodeLocation.getLineNumber()).as("line number").isEqualTo(7);
        assertThat(sourceCodeLocation.getSourceFileName()).as("source file name").isEqualTo("Object.java");
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
