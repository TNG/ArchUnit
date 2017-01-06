package com.tngtech.archunit.lang.conditions;

import java.lang.annotation.Retention;

import com.tngtech.archunit.core.JavaClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static com.tngtech.archunit.core.JavaFieldAccess.AccessType.SET;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.accessType;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.annotatedWith;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.ownerAndNameAre;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.ownerIs;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.targetTypeResidesIn;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ArchPredicatesTest {
    @Rule
    public final MockitoRule mockitoRule = MockitoJUnit.rule();

    @Mock
    private JavaClass mockClass;

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
    public void descriptions() {
        assertThat(annotatedWith(Rule.class).getDescription())
                .isEqualTo("annotated with @Rule");

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