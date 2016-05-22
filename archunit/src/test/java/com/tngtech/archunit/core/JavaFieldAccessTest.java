package com.tngtech.archunit.core;

import com.tngtech.archunit.core.JavaFieldAccess.AccessType;
import org.junit.Test;

import static com.tngtech.archunit.core.JavaFieldAccess.AccessType.GET;
import static com.tngtech.archunit.core.JavaFieldAccess.AccessType.SET;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaFieldAccessTest {
    @Test
    public void equals_should_work() throws Exception {
        JavaClass clazz = new JavaClass.Builder().withType(SomeClass.class).build();
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
                .withField(field(clazz, "intField"))
                .build());
        JavaFieldAccess otherCaller = new JavaFieldAccess(stringFieldAccessRecordBuilder(clazz)
                .withCaller(method(clazz, "accessInt"))
                .build());

        assertThat(access).isNotEqualTo(otherAccessType);
        assertThat(access).isNotEqualTo(otherLineNumber);
        assertThat(access).isNotEqualTo(otherTarget);
        assertThat(access).isNotEqualTo(otherCaller);
    }

    @Test
    public void fieldAccess_should_have_same_name_as_target() throws Exception {

        JavaClass clazz = new JavaClass.Builder().withType(SomeClass.class).build();

        JavaFieldAccess access = new JavaFieldAccess(stringFieldAccessRecordBuilder(clazz)
                .withCaller(accessFieldMethod(clazz))
                .build());

        assertThat(access.getName()).isEqualTo(access.getTarget().getName());
    }

    private TestFieldAccessRecord.Builder stringFieldAccessRecordBuilder(JavaClass clazz) throws NoSuchFieldException {
        return stringFieldAccess(clazz, "stringField");
    }

    private TestFieldAccessRecord.Builder stringFieldAccess(JavaClass clazz, String name) throws NoSuchFieldException {
        return new TestFieldAccessRecord.Builder()
                .withField(field(clazz, name))
                .withAccessType(GET)
                .withLineNumber(31);
    }

    private JavaField field(JavaClass clazz, String name) throws NoSuchFieldException {
        return new JavaField.Builder()
                .withField(SomeClass.class.getDeclaredField(name))
                .build(clazz);
    }

    private JavaMethod accessFieldMethod(JavaClass clazz) throws NoSuchMethodException {
        return method(clazz, "accessStringField");
    }

    private JavaMethod method(JavaClass clazz, String name) throws NoSuchMethodException {
        return new JavaMethod.Builder()
                .withMethod(SomeClass.class.getDeclaredMethod(name))
                .build(clazz);
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
        private final JavaMethodLike<?, ?> caller;
        private final JavaField field;
        private final int lineNumber;

        private TestFieldAccessRecord(Builder builder) {
            accessType = builder.accessType;
            caller = builder.caller;
            field = builder.field;
            lineNumber = builder.lineNumber;
        }

        @Override
        public AccessType getAccessType() {
            return accessType;
        }

        @Override
        public JavaMethodLike<?, ?> getCaller() {
            return caller;
        }

        @Override
        public JavaField getTarget() {
            return field;
        }

        @Override
        public int getLineNumber() {
            return lineNumber;
        }

        public static final class Builder {
            private AccessType accessType;
            private JavaMethodLike<?, ?> caller;
            private JavaField field;
            private int lineNumber;

            public Builder() {
            }

            public Builder withAccessType(AccessType accessType) {
                this.accessType = accessType;
                return this;
            }

            public Builder withCaller(JavaMethodLike<?, ?> caller) {
                this.caller = caller;
                return this;
            }

            public Builder withField(JavaField field) {
                this.field = field;
                return this;
            }

            public Builder withLineNumber(int lineNumber) {
                this.lineNumber = lineNumber;
                return this;
            }

            public TestFieldAccessRecord build() {
                return new TestFieldAccessRecord(this);
            }
        }
    }
}