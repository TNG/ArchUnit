package com.tngtech.archunit.core;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.JavaAccess.Functions.Get;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaAccessBuilder;
import com.tngtech.archunit.core.importer.testexamples.SomeClass;
import com.tngtech.archunit.core.importer.testexamples.SomeEnum;
import org.junit.Test;

import static com.tngtech.archunit.core.TestUtils.javaClassViaReflection;
import static com.tngtech.archunit.core.TestUtils.javaClassesViaReflection;
import static com.tngtech.archunit.core.TestUtils.javaMethodViaReflection;
import static com.tngtech.archunit.core.TestUtils.resolvedTargetFrom;
import static com.tngtech.archunit.core.TestUtils.simulateCall;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaAccessTest {
    @Test
    public void when_the_origin_is_an_inner_class_the_toplevel_class_is_displayed_as_location() {
        TestJavaAccess access = javaAccessFrom(javaClassViaReflection(SomeClass.Inner.class), "foo")
                .to(SomeEnum.class, "bar")
                .inLineNumber(7);

        assertThat(access.getDescription()).contains("(SomeClass.java:7)");
    }

    @Test
    public void location_of_origin_of_deeper_inner_class_hierarchies() {
        JavaClass innerClass = javaClassesViaReflection(SomeClass.Inner.InnerInner.class, SomeClass.Inner.class, SomeClass.class)
                .get(SomeClass.Inner.InnerInner.class);
        TestJavaAccess access = javaAccessFrom(innerClass, "bar")
                .to(SomeEnum.class, "bar")
                .inLineNumber(7);

        assertThat(access.getDescription()).contains("(SomeClass.java:7)");
    }

    @Test
    public void get_functions() {
        JavaAccess<?> access = simulateCall().from(javaMethodViaReflection(getClass(), "toString"), 5)
                .to(javaMethodViaReflection(getClass(), "hashCode"));

        assertThat(Get.origin().apply(access)).isEqualTo(access.getOrigin());
        assertThat(Get.target().apply(access)).isEqualTo(access.getTarget());
    }

    @Test
    public void origin_predicate() {
        DescribedPredicate<JavaAccess<?>> predicate =
                JavaAccess.Predicates.origin(DescribedPredicate.<JavaCodeUnit>alwaysTrue().as("some text"));

        assertThat(predicate.getDescription()).isEqualTo("origin some text");
        assertThat(predicate.apply(anyAccess())).as("predicate matches").isTrue();

        predicate = JavaAccess.Predicates.origin(DescribedPredicate.<JavaCodeUnit>alwaysFalse());
        assertThat(predicate.apply(anyAccess())).as("predicate matches").isFalse();
    }

    private TestJavaAccess anyAccess() {
        return javaAccessFrom(javaClassViaReflection(SomeClass.Inner.class), "foo")
                .to(SomeEnum.class, "bar")
                .inLineNumber(7);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }

    private static TestJavaAccess.Creator javaAccessFrom(JavaClass owner, String name) {
        return new TestJavaAccess.Creator(owner, name);
    }

    private static class TestJavaAccess extends JavaAccess<MethodCallTarget> {
        TestJavaAccess(JavaMethod origin, JavaMethod target, int lineNumber) {
            super(new Builder(origin, resolvedTargetFrom(target), lineNumber));
        }

        @Override
        protected String descriptionTemplate() {
            return "";
        }

        public static class Creator {
            private final JavaMethod origin;
            private JavaMethod target;

            private Creator(JavaClass owner, String name) {
                this.origin = javaMethodViaReflection(owner, name);
            }

            public Creator to(Class<?> owner, String name) {
                this.target = javaMethodViaReflection(owner, name);
                return this;
            }

            TestJavaAccess inLineNumber(int lineNumber) {
                return new TestJavaAccess(origin, target, lineNumber);
            }
        }

        private static class Builder extends JavaAccessBuilder<MethodCallTarget, Builder> {
            public Builder(JavaMethod origin, MethodCallTarget methodCallTarget, int lineNumber) {
                withOrigin(origin).withTarget(methodCallTarget).withLineNumber(lineNumber);
            }
        }
    }
}