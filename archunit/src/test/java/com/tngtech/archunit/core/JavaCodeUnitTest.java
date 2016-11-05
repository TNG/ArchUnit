package com.tngtech.archunit.core;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import static com.tngtech.archunit.core.TestUtils.javaMethod;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaCodeUnitTest {
    @Test
    public void hasParameters() {
        JavaMethod method = javaMethod(SomeClass.class, "withArgs", Object.class, String.class);

        DescribedPredicate<JavaCodeUnit<?, ?>> predicate =
                JavaCodeUnit.hasParameters(TypeDetails.allOf(Collections.<Class<?>>singletonList(Object.class)));

        assertThat(predicate.apply(method)).as("Predicate matches").isFalse();
        assertThat(predicate.getDescription()).isEqualTo("has parameters [Object.class]");

        predicate =
                JavaCodeUnit.hasParameters(TypeDetails.allOf(Arrays.asList(Object.class, String.class)));

        assertThat(predicate.apply(method)).as("Predicate matches").isTrue();
        assertThat(predicate.getDescription()).isEqualTo("has parameters [Object.class, String.class]");
    }

    private static class SomeClass {
        void withArgs(Object arg, String stringArg) {
        }
    }
}