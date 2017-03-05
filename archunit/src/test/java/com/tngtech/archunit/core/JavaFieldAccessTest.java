package com.tngtech.archunit.core;

import java.util.EnumSet;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.AccessTarget.FieldAccessTarget;
import com.tngtech.archunit.core.JavaFieldAccess.AccessType;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.JavaFieldAccess.AccessType.GET;
import static com.tngtech.archunit.core.JavaFieldAccess.AccessType.SET;
import static com.tngtech.archunit.core.JavaFieldAccess.Predicates.accessType;
import static com.tngtech.archunit.core.JavaFieldAccess.Predicates.target;
import static com.tngtech.archunit.core.TestUtils.javaClassViaReflection;
import static com.tngtech.archunit.core.TestUtils.javaFieldViaReflection;
import static com.tngtech.archunit.core.TestUtils.javaMethodViaReflection;
import static com.tngtech.archunit.core.TestUtils.targetFrom;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class JavaFieldAccessTest {
    @Test
    public void equals_should_work() throws Exception {
        JavaClass clazz = javaClassViaReflection(SomeClass.class);
        JavaFieldAccess access = new JavaFieldAccess(stringFieldAccessRecordBuilder(clazz)
                .withCaller(accessFieldMethod(clazz))
                .build());
        JavaFieldAccess equalAccess = new JavaFieldAccess(stringFieldAccessRecordBuilder(clazz)
                .withCaller(accessFieldMethod(clazz))
                .build());

        assertThat(access).isEqualTo(access);
        assertThat(access.hashCode()).isEqualTo(access.hashCode());
        assertThat(access).isEqualTo(equalAccess);
        assertThat(access.hashCode()).isEqualTo(equalAccess.hashCode());

        JavaFieldAccess otherAccessType = new JavaFieldAccess(stringFieldAccessRecordBuilder(clazz)
                .withAccessType(SET)
                .withCaller(accessFieldMethod(clazz))
                .build());
        JavaFieldAccess otherLineNumber = new JavaFieldAccess(stringFieldAccessRecordBuilder(clazz)
                .withCaller(accessFieldMethod(clazz))
                .withLineNumber(999)
                .build());
        JavaFieldAccess otherTarget = new JavaFieldAccess(stringFieldAccessRecordBuilder(clazz)
                .withCaller(accessFieldMethod(clazz))
                .withField(javaFieldViaReflection(clazz, "intField"))
                .build());
        JavaFieldAccess otherCaller = new JavaFieldAccess(stringFieldAccessRecordBuilder(clazz)
                .withCaller(javaMethodViaReflection(clazz, "accessInt"))
                .build());

        assertThat(access).isNotEqualTo(otherAccessType);
        assertThat(access).isNotEqualTo(otherLineNumber);
        assertThat(access).isNotEqualTo(otherTarget);
        assertThat(access).isNotEqualTo(otherCaller);
    }

    @Test
    public void fieldAccess_should_have_same_name_as_target() throws Exception {

        JavaClass clazz = javaClassViaReflection(SomeClass.class);

        JavaFieldAccess access = new JavaFieldAccess(stringFieldAccessRecordBuilder(clazz)
                .withCaller(accessFieldMethod(clazz))
                .build());

        assertThat(access.getName()).isEqualTo(access.getTarget().getName());
    }

    @DataProvider
    public static Object[][] accessTypes() {
        return $$($(GET), $(SET));
    }

    @Test
    @UseDataProvider("accessTypes")
    public void predicate_access_type(AccessType accessType) throws Exception {
        assertThat(accessType(accessType).apply(stringFieldAccess(accessType)))
                .as("Predicate matches").isTrue();
        assertThat(accessType(accessType).apply(stringFieldAccess(not(accessType))))
                .as("Predicate matches").isFalse();

        assertThat(accessType(accessType).getDescription())
                .as("Predicate description").isEqualTo("access type " + accessType);
    }

    @Test
    public void predicate_field_access_target_by_predicate() throws Exception {
        assertThat(target(DescribedPredicate.<FieldAccessTarget>alwaysTrue())
                .apply(stringFieldAccess(GET))).as("Predicate matches").isTrue();
        assertThat(target(DescribedPredicate.<FieldAccessTarget>alwaysFalse())
                .apply(stringFieldAccess(GET))).as("Predicate matches").isFalse();

        assertThat(target(DescribedPredicate.<FieldAccessTarget>alwaysTrue().as("any message"))
                .getDescription()).as("description").isEqualTo("field access target any message");
    }

    private AccessType not(AccessType accessType) {
        return getOnlyElement(EnumSet.complementOf(EnumSet.of(accessType)));
    }

    private TestFieldAccessRecord.Builder stringFieldAccessRecordBuilder(JavaClass clazz) throws NoSuchFieldException {
        return stringFieldAccessBuilder(clazz, "stringField");
    }

    private JavaFieldAccess stringFieldAccess(AccessType accessType) throws Exception {
        JavaClass clazz = javaClassViaReflection(SomeClass.class);
        return new JavaFieldAccess(
                new TestFieldAccessRecord.Builder()
                        .withCaller(accessFieldMethod(clazz))
                        .withTarget(targetFrom(javaFieldViaReflection(clazz, "stringField")))
                        .withAccessType(accessType)
                        .withLineNumber(31)
                        .build());
    }

    private TestFieldAccessRecord.Builder stringFieldAccessBuilder(JavaClass clazz, String name) throws NoSuchFieldException {
        return stringFieldAccessBuilder(targetFrom(javaFieldViaReflection(clazz, name)));
    }

    private JavaFieldAccess defaultFieldAccessTo(FieldAccessTarget target) throws Exception {
        return new JavaFieldAccess(stringFieldAccessBuilder(target)
                .withCaller(accessFieldMethod(javaClassViaReflection(SomeClass.class)))
                .withAccessType(GET)
                .build());
    }

    private TestFieldAccessRecord.Builder stringFieldAccessBuilder(FieldAccessTarget target) throws NoSuchFieldException {
        return new TestFieldAccessRecord.Builder()
                .withTarget(target)
                .withAccessType(GET)
                .withLineNumber(31);
    }

    private JavaMethod accessFieldMethod(JavaClass clazz) throws NoSuchMethodException {
        return javaMethodViaReflection(clazz, "accessStringField");
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

    private static class TestFieldAccessRecord implements AccessRecord.FieldAccessRecord {
        private final AccessType accessType;
        private final JavaCodeUnit caller;
        private FieldAccessTarget fieldAccessTarget;
        private final int lineNumber;

        private TestFieldAccessRecord(Builder builder) {
            accessType = builder.accessType;
            caller = builder.caller;
            fieldAccessTarget = builder.target;
            lineNumber = builder.lineNumber;
        }

        @Override
        public AccessType getAccessType() {
            return accessType;
        }

        @Override
        public JavaCodeUnit getCaller() {
            return caller;
        }

        @Override
        public FieldAccessTarget getTarget() {
            return fieldAccessTarget;
        }

        @Override
        public int getLineNumber() {
            return lineNumber;
        }

        static final class Builder {
            private AccessType accessType;
            private JavaCodeUnit caller;
            public FieldAccessTarget target;
            private int lineNumber;

            Builder withAccessType(AccessType accessType) {
                this.accessType = accessType;
                return this;
            }

            Builder withCaller(JavaCodeUnit caller) {
                this.caller = caller;
                return this;
            }

            Builder withField(JavaField field) {
                return withTarget(targetFrom(field));
            }

            Builder withTarget(FieldAccessTarget target) {
                this.target = target;
                return this;
            }

            Builder withLineNumber(int lineNumber) {
                this.lineNumber = lineNumber;
                return this;
            }

            TestFieldAccessRecord build() {
                return new TestFieldAccessRecord(this);
            }
        }
    }
}