package com.tngtech.archunit.lang.conditions;

import java.lang.annotation.Retention;

import com.tngtech.archunit.core.JavaClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.tngtech.archunit.core.JavaFieldAccess.AccessType.SET;
import static com.tngtech.archunit.core.TestUtils.javaClassViaReflection;
import static com.tngtech.archunit.core.TestUtils.predicateWithDescription;
import static com.tngtech.archunit.core.properties.HasName.Predicates.withNameMatching;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.accessType;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.annotatedWith;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.ownerAndNameAre;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.ownerIs;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.resideIn;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.targetTypeResidesIn;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.theHierarchyOf;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.theHierarchyOfAClassThat;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ArchPredicatesTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private JavaClass mockClass;

    @Test
    public void matches_class_package() {
        when(mockClass.getPackage()).thenReturn("some.arbitrary.pkg");

        assertThat(resideIn("some..pkg").apply(mockClass)).as("package matches").isTrue();
    }

    @Test
    public void mismatches_class_package() {
        when(mockClass.getPackage()).thenReturn("wrong.arbitrary.pkg");

        assertThat(resideIn("some..pkg").apply(mockClass)).as("package matches").isFalse();
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void matches_annotation() {
        when(mockClass.isAnnotatedWith(SomeAnnotation.class)).thenReturn(true);

        assertThat(annotatedWith(SomeAnnotation.class).apply(mockClass))
                .as("annotated class matches")
                .isTrue();

        when(mockClass.isAnnotatedWith(SomeAnnotation.class)).thenReturn(false);

        assertThat(annotatedWith(SomeAnnotation.class).apply(mockClass))
                .as("annotated class matches")
                .isFalse();
    }

    @Test
    public void inTheHierarchyOfAClass_matches_class_itself() {
        assertThat(theHierarchyOfAClassThat(withNameMatching(".*Class")).apply(javaClassViaReflection(AnyClass.class)))
                .as("class itself matches the predicate").isTrue();
    }

    @Test
    public void inTheHierarchyOfAClass_matches_subclass() {
        assertThat(theHierarchyOfAClassThat(withNameMatching(".*Any.*")).apply(javaClassViaReflection(SubClass.class)))
                .as("subclass matches the predicate").isTrue();
    }

    @Test
    public void inTheHierarchyOfAClass_does_not_match_superclass() {
        assertThat(theHierarchyOfAClassThat(withNameMatching(".*Any.*")).apply(javaClassViaReflection(Object.class)))
                .as("superclass matches the predicate").isFalse();
    }

    @Test
    public void descriptions() {
        assertThat(resideIn("..any..").getDescription())
                .isEqualTo("reside in '..any..'");

        assertThat(annotatedWith(Rule.class).getDescription())
                .isEqualTo("annotated with @Rule");

        assertThat(theHierarchyOf(Object.class).getDescription())
                .isEqualTo("the hierarchy of Object.class");

        assertThat(theHierarchyOfAClassThat(predicateWithDescription("something")).getDescription())
                .isEqualTo("the hierarchy of a class that something");

        assertThat(ownerAndNameAre(System.class, "out").getDescription())
                .isEqualTo("owner is java.lang.System and name is 'out'");

        assertThat(ownerIs(System.class).getDescription())
                .isEqualTo("owner is java.lang.System");

        assertThat(accessType(SET).getDescription())
                .isEqualTo("access type " + SET);

        assertThat(targetTypeResidesIn("..any..").getDescription())
                .isEqualTo("target type resides in '..any..'");
    }

    @Retention(RUNTIME)
    @interface SomeAnnotation {
    }

    private static class AnyClass {
    }

    private static class SubClass extends AnyClass {
    }
}