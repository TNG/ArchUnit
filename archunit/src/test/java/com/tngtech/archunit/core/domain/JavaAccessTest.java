package com.tngtech.archunit.core.domain;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.domain.JavaAccess.Functions.Get;
import com.tngtech.archunit.core.importer.testexamples.SomeClass;
import com.tngtech.archunit.core.importer.testexamples.SomeEnum;
import org.junit.Test;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysFalse;
import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.newMethodCallBuilder;
import static com.tngtech.archunit.core.domain.TestUtils.resolvedTargetFrom;
import static com.tngtech.archunit.core.domain.TestUtils.simulateCall;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatConversionOf;
import static com.tngtech.archunit.testutil.Assertions.assertThatDependency;

public class JavaAccessTest {
    @Test
    public void when_the_origin_is_an_inner_class_the_toplevel_class_is_displayed_as_location() {
        TestJavaAccess access = javaAccessFrom(importClassWithContext(SomeClass.Inner.class), "foo")
                .to(SomeEnum.class, "bar")
                .inLineNumber(7);

        assertThat(access.getDescription()).contains("(SomeClass.java:7)");
    }

    @Test
    public void location_of_origin_of_deeper_inner_class_hierarchies() {
        JavaClass innerClass = importClassesWithContext(SomeClass.Inner.InnerInner.class, SomeClass.Inner.class, SomeClass.class)
                .get(SomeClass.Inner.InnerInner.class);
        TestJavaAccess access = javaAccessFrom(innerClass, "bar")
                .to(SomeEnum.class, "bar")
                .inLineNumber(7);

        assertThat(access.getDescription()).contains("(SomeClass.java:7)");
    }

    @Test
    public void get_functions() {
        JavaAccess<?> access = simulateCall().from(importClassWithContext(getClass()).getMethod("toString"), 5)
                .to(importClassWithContext(getClass()).getMethod("hashCode"));

        assertThat(Get.origin().apply(access)).isEqualTo(access.getOrigin());
        assertThat(Get.target().apply(access)).isEqualTo(access.getTarget());
    }

    @Test
    public void origin_predicate() {
        DescribedPredicate<JavaAccess<?>> predicate =
                JavaAccess.Predicates.origin(DescribedPredicate.<JavaCodeUnit>alwaysTrue().as("some text"));

        assertThat(predicate)
                .hasDescription("origin some text")
                .accepts(anyAccess());

        predicate = JavaAccess.Predicates.origin(alwaysFalse());
        assertThat(predicate).rejects(anyAccess());
    }

    @Test
    public void target_predicate() {
        DescribedPredicate<JavaAccess<?>> predicate =
                JavaAccess.Predicates.target(DescribedPredicate.<AccessTarget>alwaysTrue().as("some text"));

        assertThat(predicate)
                .hasDescription("target some text")
                .accepts(anyAccess());

        predicate = JavaAccess.Predicates.target(alwaysFalse());
        assertThat(predicate).rejects(anyAccess());
    }

    @Test
    public void targetOwner_predicate() {
        DescribedPredicate<JavaAccess<?>> predicate =
                JavaAccess.Predicates.targetOwner(DescribedPredicate.<JavaClass>alwaysTrue().as("some text"));

        assertThat(predicate)
                .hasDescription("owner some text")
                .accepts(anyAccess());

        predicate = JavaAccess.Predicates.targetOwner(alwaysFalse());
        assertThat(predicate).rejects(anyAccess());
    }

    @Test
    public void convertTo() {
        TestJavaAccess access = javaAccessFrom(importClassWithContext(String.class), "toString")
                .to(Object.class, "toString")
                .inLineNumber(11);

        assertThatConversionOf(access)
                .satisfiesStandardConventions()
                .isPossibleToSingleElement(Dependency.class, dependency -> {
                    assertThatDependency(dependency)
                            .matches(String.class, Object.class)
                            .hasDescription(access.getDescription());
                });
    }

    private TestJavaAccess anyAccess() {
        return javaAccessFrom(importClassWithContext(SomeClass.Inner.class), "foo")
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
            super(newMethodCallBuilder(origin, resolvedTargetFrom(target), lineNumber));
        }

        @Override
        protected String descriptionVerb() {
            return "accesses";
        }

        public static class Creator {
            private final JavaMethod origin;
            private JavaMethod target;

            private Creator(JavaClass owner, String name) {
                this.origin = owner.getMethod(name);
            }

            public Creator to(Class<?> owner, String name) {
                this.target = importClassWithContext(owner).getMethod(name);
                return this;
            }

            TestJavaAccess inLineNumber(int lineNumber) {
                return new TestJavaAccess(origin, target, lineNumber);
            }
        }
    }
}
