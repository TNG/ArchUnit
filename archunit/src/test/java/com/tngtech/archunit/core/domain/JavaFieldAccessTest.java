package com.tngtech.archunit.core.domain;

import java.util.EnumSet;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType;
import com.tngtech.archunit.core.importer.JavaFieldAccessTestBuilder;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType.GET;
import static com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType.SET;
import static com.tngtech.archunit.core.domain.JavaFieldAccess.Predicates.accessType;
import static com.tngtech.archunit.core.domain.JavaFieldAccess.Predicates.target;
import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.targetFrom;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;


@RunWith(DataProviderRunner.class)
public class JavaFieldAccessTest {
    @Test
    public void equals_should_work() throws Exception {
        JavaClass clazz = importClassWithContext(SomeClass.class);
        JavaFieldAccess access = stringFieldAccessRecordBuilder(clazz)
                .withOrigin(accessFieldMethod(clazz))
                .build();
        JavaFieldAccess equalAccess = stringFieldAccessRecordBuilder(clazz)
                .withOrigin(accessFieldMethod(clazz))
                .build();

        assertThat(access).isEqualTo(access);
        assertThat(access.hashCode()).isEqualTo(access.hashCode());
        assertThat(access).isEqualTo(equalAccess);
        assertThat(access.hashCode()).isEqualTo(equalAccess.hashCode());

        JavaFieldAccess otherAccessType = stringFieldAccessRecordBuilder(clazz)
                .withAccessType(SET)
                .withOrigin(accessFieldMethod(clazz))
                .build();
        JavaFieldAccess otherLineNumber = stringFieldAccessRecordBuilder(clazz)
                .withOrigin(accessFieldMethod(clazz))
                .withLineNumber(999)
                .build();
        JavaFieldAccess otherTarget = stringFieldAccessRecordBuilder(clazz)
                .withOrigin(accessFieldMethod(clazz))
                .withTarget(targetFrom(clazz.getField("intField")))
                .build();
        JavaFieldAccess otherCaller = stringFieldAccessRecordBuilder(clazz)
                .withOrigin(clazz.getMethod("accessInt"))
                .build();

        assertThat(access).isNotEqualTo(otherAccessType);
        assertThat(access).isNotEqualTo(otherLineNumber);
        assertThat(access).isNotEqualTo(otherTarget);
        assertThat(access).isNotEqualTo(otherCaller);
    }

    @Test
    public void fieldAccess_should_have_same_name_as_target() throws Exception {

        JavaClass clazz = importClassWithContext(SomeClass.class);

        JavaFieldAccess access = stringFieldAccessRecordBuilder(clazz)
                .withOrigin(accessFieldMethod(clazz))
                .build();

        assertThat(access.getName()).isEqualTo(access.getTarget().getName());
    }

    @DataProvider
    public static Object[][] accessTypes() {
        return $$($(GET), $(SET));
    }

    @Test
    @UseDataProvider("accessTypes")
    public void predicate_access_type(AccessType accessType) throws Exception {
        assertThat(accessType(accessType))
                .accepts(stringFieldAccess(accessType))
                .rejects(stringFieldAccess(not(accessType)))
                .hasDescription("access type " + accessType);
    }

    @Test
    public void predicate_field_access_target_by_predicate() throws Exception {
        assertThat(target(DescribedPredicate.<FieldAccessTarget>alwaysTrue()))
                .accepts(stringFieldAccess(GET));
        assertThat(target(DescribedPredicate.<FieldAccessTarget>alwaysFalse().as("any message")))
                .rejects(stringFieldAccess(GET))
                .hasDescription("target any message");
    }

    private AccessType not(AccessType accessType) {
        return getOnlyElement(EnumSet.complementOf(EnumSet.of(accessType)));
    }

    private JavaFieldAccessTestBuilder stringFieldAccessRecordBuilder(JavaClass clazz) throws NoSuchFieldException {
        return stringFieldAccessBuilder(clazz, "stringField");
    }

    private JavaFieldAccess stringFieldAccess(AccessType accessType) throws Exception {
        JavaClass clazz = importClassWithContext(SomeClass.class);
        return new JavaFieldAccessTestBuilder()
                .withOrigin(accessFieldMethod(clazz))
                .withTarget(targetFrom(clazz.getField("stringField")))
                .withAccessType(accessType)
                .withLineNumber(31)
                .build();
    }

    private JavaFieldAccessTestBuilder stringFieldAccessBuilder(JavaClass clazz, String name) throws NoSuchFieldException {
        return stringFieldAccessBuilder(targetFrom(clazz.getField(name)));
    }

    private JavaFieldAccessTestBuilder stringFieldAccessBuilder(FieldAccessTarget target) throws NoSuchFieldException {
        return new JavaFieldAccessTestBuilder()
                .withTarget(target)
                .withAccessType(GET)
                .withLineNumber(31);
    }

    private JavaMethod accessFieldMethod(JavaClass clazz) throws NoSuchMethodException {
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