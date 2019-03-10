package com.tngtech.archunit.library.dependencies;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.DescribedIterable;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.TestUtils;
import com.tngtech.archunit.testutil.Assertions;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.domain.TestUtils.dependencyFrom;
import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.simulateCall;
import static com.tngtech.archunit.testutil.Assertions.assertThatClasses;
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
    public void matching_description() {
        JavaClasses classes = importClassesWithContext(Object.class);

        Slices.Transformer transformer = Slices.matching("java.(*)..");
        assertThat(transformer.getDescription()).isEqualTo("slices matching 'java.(*)..'");

        Slices slices = transformer.transform(classes);
        assertThat(slices.getDescription()).isEqualTo("slices matching 'java.(*)..'");

        slices = transformer.that(DescribedPredicate.<Slice>alwaysTrue().as("changed")).transform(classes);
        assertThat(slices.getDescription()).isEqualTo("slices matching 'java.(*)..' that changed");
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

    @Test
    public void slices_from_identifier() {
        Slices slices = Slices.assignedFrom(assignmentOfJavaLangAndUtil("some description"))
                .namingSlices("Any $1 - $2")
                .transform(importClassesWithContext(Object.class, Number.class, List.class, Collection.class, File.class));

        assertThat(slices.getDescription()).isEqualTo("slices assigned from some description");
        assertThat(slices).extractingResultOf("getDescription").containsOnly("Any Lang - $2", "Any Adjusted - Util");
        assertThat(slices).hasSize(2);
        assertThatClasses(getSliceOf(Object.class, slices)).contain(Number.class);
        assertThatClasses(getSliceOf(List.class, slices)).contain(Collection.class);
        Assertions.assertThat(tryGetSliceOf(File.class, slices))
                .as("Slice of class java.io.File (which should be missing from the assignment)")
                .isAbsent();
    }

    private Slice getSliceOf(Class<?> clazz, Slices slices) {
        return tryGetSliceOf(clazz, slices).get();
    }

    private Optional<Slice> tryGetSliceOf(Class<?> clazz, Slices slices) {
        for (Slice slice : slices) {
            for (JavaClass javaClass : slice) {
                if (javaClass.isEquivalentTo(clazz)) {
                    return Optional.of(slice);
                }
            }
        }
        return Optional.absent();
    }

    private SliceAssignment assignmentOfJavaLangAndUtil(final String description) {
        return new SliceAssignment() {
            @Override
            public String getDescription() {
                return description;
            }

            @Override
            public SliceIdentifier getIdentifierOf(JavaClass javaClass) {
                if (javaClass.getPackageName().startsWith("java.lang")) {
                    return SliceIdentifier.of("Lang");
                }
                if (javaClass.getPackageName().startsWith("java.util")) {
                    return SliceIdentifier.of("Adjusted", "Util");
                }
                return SliceIdentifier.ignore();
            }
        };
    }
}