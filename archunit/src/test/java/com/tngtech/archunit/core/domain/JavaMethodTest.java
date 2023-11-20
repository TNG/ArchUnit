package com.tngtech.archunit.core.domain;

import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.importMethod;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaMethodTest {

    @Test
    public void isOverriding_returns_true_for_implemented_interface_method() {
        assertThat(importMethod(SomeClass.class, "doSomething").isOverriding()).isTrue();
    }

    @Test
    public void isOverriding_returns_false_for_interface_method() {
        assertThat(importMethod(SomeInterface.class, "doSomething").isOverriding()).isFalse();
    }

    @Test
    public void isOverriding_returns_true_for_overriding_method_of_java_lang_Object() {
        assertThat(importMethod(SomeClass.class, "toString").isOverriding()).isTrue();
    }

    @Test
    public void isOverriding_returns_false_for_non_overriding_method() {
        assertThat(importMethod(SomeClass.class, "doSomethingElse").isOverriding()).isFalse();
    }

    @Test
    public void isOverriding_returns_false_for_non_overriding_overloaded_method() {
        assertThat(importMethod(SomeClass.class, "doSomething", Object.class).isOverriding()).isFalse();
    }

    @SuppressWarnings("unused")
    private interface SomeInterface {

        void doSomething();
    }

    @SuppressWarnings("unused")
    private static class SomeClass implements SomeInterface {

        @Override
        public void doSomething() {
        }

        public void doSomething(Object o) {
        }

        public void doSomethingElse() {
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }
}
