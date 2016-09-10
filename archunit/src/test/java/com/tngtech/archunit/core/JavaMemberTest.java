package com.tngtech.archunit.core;

import org.junit.Test;

import static com.tngtech.archunit.core.JavaMember.modifier;
import static com.tngtech.archunit.core.TestUtils.javaMethod;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class JavaMemberTest {
    @Test
    public void modifier_predicate() {
        assertThat(modifier(JavaModifier.PRIVATE).apply(javaMethod(SomeClass.class, "isPrivate")))
                .as("Predicate matches").isTrue();
        assertThat(modifier(JavaModifier.PRIVATE).apply(javaMethod(SomeClass.class, "isNotPrivate")))
                .as("Predicate matches").isFalse();
        assertThat(modifier(JavaModifier.PRIVATE).getDescription()).isEqualTo("modifier PRIVATE");
    }

    private static class SomeClass {
        private void isPrivate() {
        }

        void isNotPrivate() {
        }
    }
}