package com.tngtech.archunit.core;

import com.tngtech.archunit.core.testexamples.SomeClass;
import com.tngtech.archunit.core.testexamples.SomeEnum;
import org.junit.Test;

import static com.tngtech.archunit.core.TestUtils.javaClass;
import static com.tngtech.archunit.core.TestUtils.javaMethod;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaAccessTest {
    @Test
    public void when_the_origin_is_an_inner_class_the_toplevel_class_is_displayed_as_location() {
        TestJavaAccess access = javaAccessFrom(javaClass(SomeClass.Inner.class), "foo")
                .to(SomeEnum.class, "bar")
                .inLineNumber(7);

        assertThat(access.getDescription()).contains("(SomeClass.java:7)");
    }

    @Test
    public void location_of_origin_of_deeper_inner_class_hierarchies() {
        TestJavaAccess access = javaAccessFrom(javaClass(SomeClass.Inner.InnerInner.class), "bar")
                .to(SomeEnum.class, "bar")
                .inLineNumber(7);

        assertThat(access.getDescription()).contains("(SomeClass.java:7)");
    }

    private static TestJavaAccess.Creator javaAccessFrom(JavaClass owner, String name) {
        return new TestJavaAccess.Creator(owner, name);
    }

    private static class TestJavaAccess extends JavaAccess<JavaMethod> {
        public TestJavaAccess(JavaMethod origin, JavaMethod target, int lineNumber) {
            super(origin, target, lineNumber);
        }

        @Override
        protected String descriptionTemplate() {
            return "";
        }

        private static class Creator {
            private final JavaMethod origin;
            private JavaMethod target;

            public Creator(JavaClass owner, String name) {
                this.origin = javaMethod(owner, name);
            }

            public Creator to(Class<?> owner, String name) {
                this.target = javaMethod(owner, name);
                return this;
            }

            public TestJavaAccess inLineNumber(int lineNumber) {
                return new TestJavaAccess(origin, target, lineNumber);
            }
        }
    }
}