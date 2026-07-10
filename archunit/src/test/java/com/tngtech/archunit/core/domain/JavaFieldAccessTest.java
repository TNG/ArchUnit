package com.tngtech.archunit.core.domain;

import java.util.EnumSet;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.importer.JavaFieldAccessTestBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType.GET;
import static com.tngtech.archunit.core.domain.JavaFieldAccess.Predicates.accessType;
import static com.tngtech.archunit.core.domain.JavaFieldAccess.Predicates.target;
import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.targetFrom;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class JavaFieldAccessTest {
    @Test
    public void equals_of_JavaAccess_should_be_identity_equals() throws Exception {
        JavaClass clazz = importClassWithContext(SomeClass.class);
        JavaFieldAccess access = stringFieldAccessRecordBuilder(clazz)
                .withOrigin(accessFieldMethod(clazz))
                .build();
        JavaFieldAccess samePropertiesAccess = stringFieldAccessRecordBuilder(clazz)
                .withOrigin(accessFieldMethod(clazz))
                .build();

        assertThat(access).isEqualTo(access);
        assertThat(access.hashCode()).isEqualTo(access.hashCode());
        assertThat(access).isNotEqualTo(samePropertiesAccess);
    }

    @Test
    public void fieldAccess_should_have_same_name_as_target() throws Exception {
        JavaClass clazz = importClassWithContext(SomeClass.class);

        JavaFieldAccess access = stringFieldAccessRecordBuilder(clazz)
                .withOrigin(accessFieldMethod(clazz))
                .build();

        assertThat(access.getName()).isEqualTo(access.getTarget().getName());
    }

    @ParameterizedTest
    @EnumSource(AccessType.class)
    void predicate_access_type(AccessType accessType) {
        assertThat(accessType(accessType))
                .accepts(stringFieldAccess(accessType))
                .rejects(stringFieldAccess(not(accessType)))
                .hasDescription("access type " + accessType);
    }

    @Test
    public void predicate_field_access_target_by_predicate() {
        assertThat(target(alwaysTrue()))
                .accepts(stringFieldAccess(GET));
        assertThat(target(DescribedPredicate.<FieldAccessTarget>alwaysFalse().as("any message")))
                .rejects(stringFieldAccess(GET))
                .hasDescription("target any message");
    }

    private AccessType not(AccessType accessType) {
        return getOnlyElement(EnumSet.complementOf(EnumSet.of(accessType)));
    }

    private JavaFieldAccessTestBuilder stringFieldAccessRecordBuilder(JavaClass clazz) {
        return stringFieldAccessBuilder(clazz, "stringField");
    }

    private JavaFieldAccess stringFieldAccess(AccessType accessType) {
        JavaClass clazz = importClassWithContext(SomeClass.class);
        return new JavaFieldAccessTestBuilder()
                .withOrigin(accessFieldMethod(clazz))
                .withTarget(targetFrom(clazz.getField("stringField")))
                .withAccessType(accessType)
                .withLineNumber(31)
                .build();
    }

    private JavaFieldAccessTestBuilder stringFieldAccessBuilder(JavaClass clazz, String name) {
        return stringFieldAccessBuilder(targetFrom(clazz.getField(name)));
    }

    private JavaFieldAccessTestBuilder stringFieldAccessBuilder(FieldAccessTarget target) {
        return new JavaFieldAccessTestBuilder()
                .withTarget(target)
                .withAccessType(GET)
                .withLineNumber(31);
    }

    private JavaMethod accessFieldMethod(JavaClass clazz) {
        return clazz.getMethod("accessStringField");
    }

    private static class SomeClass {
        private String stringField;
        private int intField;

        private String accessStringField() {
            return stringField;
        }

        private int accessInt() {
            return intField;
        }
    }
}
