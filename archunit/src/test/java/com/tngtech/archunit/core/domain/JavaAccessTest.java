package com.tngtech.archunit.core.domain;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaAccess.Functions.Get;
import com.tngtech.archunit.core.importer.testexamples.SomeClass;
import com.tngtech.archunit.core.importer.testexamples.SomeEnum;
import com.tngtech.archunit.testutil.Assertions.ConversionResultAssertion;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.importClassesWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.simulateCall;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatConversionOf;

public class JavaAccessTest {
    @Test
    public void when_the_origin_is_an_inner_class_the_toplevel_class_is_displayed_as_location() {
        JavaMethodCall access = simulateCall().from(importClassWithContext(SomeClass.Inner.class), "foo")
                .inLineNumber(7)
                .to(SomeEnum.class, "bar");

        assertThat(access.getDescription()).contains("(SomeClass.java:7)");
    }

    @Test
    public void location_of_origin_of_deeper_inner_class_hierarchies() {
        JavaClass innerClass = importClassesWithContext(SomeClass.Inner.InnerInner.class, SomeClass.Inner.class, SomeClass.class)
                .get(SomeClass.Inner.InnerInner.class);
        JavaMethodCall access = simulateCall().from(innerClass, "bar")
                .inLineNumber(7)
                .to(SomeEnum.class, "bar");

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

        predicate = JavaAccess.Predicates.origin(DescribedPredicate.<JavaCodeUnit>alwaysFalse());
        assertThat(predicate).rejects(anyAccess());
    }

    @Test
    public void convertTo() {
        final JavaMethodCall access = simulateCall().from(importClassWithContext(Object.class), "toString")
                .to(String.class, "toString");

        assertThatConversionOf(access)
                .isNotPossibleTo(JavaClass.class)
                .isPossibleToSingleElement(Object.class, new ConversionResultAssertion<Object>() {
                    @Override
                    public void assertResult(Object dependency) {
                        assertThat(dependency).as("converted dependency").isInstanceOf(Dependency.class);
                    }
                })
                .isPossibleToSingleElement(Dependency.class, new ConversionResultAssertion<Dependency>() {
                    @Override
                    public void assertResult(Dependency dependency) {
                        assertThat(dependency.getOriginClass().isEquivalentTo(Object.class)).as("origin is Object.class").isTrue();
                        assertThat(dependency.getTargetClass().isEquivalentTo(String.class)).as("target is String.class").isTrue();
                        assertThat(dependency.getDescription()).as("description of converted dependency").isEqualTo(access.getDescription());
                    }
                });
    }

    private JavaMethodCall anyAccess() {
        return simulateCall().from(importClassWithContext(SomeClass.Inner.class), "foo")
                .to(SomeEnum.class, "bar");
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
