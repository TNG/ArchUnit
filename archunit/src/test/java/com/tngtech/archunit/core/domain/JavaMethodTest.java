package com.tngtech.archunit.core.domain;

import java.io.File;

import org.junit.Test;

import static com.tngtech.archunit.core.domain.TestUtils.importMethod;
import static org.assertj.core.api.Assertions.assertThat;

public class JavaMethodTest {

    @Test
    public void isOverriding_returns_true_for_implemented_interface_method() {
        assertThat(importMethod(SomeClass.class, "doSomething").isOverriding())
                .as("method is detected as overriding").isTrue();
    }

    @Test
    public void isOverriding_returns_false_for_interface_method() {
        assertThat(importMethod(SomeInterface.class, "doSomething").isOverriding())
                .as("method is detected as overriding").isFalse();
    }

    @Test
    public void isOverriding_returns_true_for_overriding_method_of_java_lang_Object() {
        assertThat(importMethod(SomeClass.class, "toString").isOverriding())
                .as("method is detected as overriding").isTrue();
    }

    @Test
    public void isOverriding_returns_false_for_non_overriding_method() {
        assertThat(importMethod(SomeClass.class, "doSomethingElse").isOverriding())
                .as("method is detected as overriding").isFalse();
    }

    @Test
    public void isOverriding_returns_false_for_non_overriding_overloaded_method() {
        assertThat(importMethod(SomeClass.class, "doSomething", Object.class).isOverriding())
                .as("method is detected as overriding").isFalse();
    }

    @Test
    public void isOverriding_returns_true_for_concrete_implementation_of_generic_interface_method() {
        assertThat(importMethod(SomeClassImplementingGenericsConcrete.class, "genericParam", Object.class).isOverriding())
                .as("method is detected as overriding").isTrue();
        assertThat(importMethod(SomeClassImplementingGenericsConcrete.class, "genericParam", String.class).isOverriding())
                .as("method is detected as overriding").isTrue();
        assertThat(importMethod(SomeClassImplementingGenericsConcrete.class, "genericReturnType").isOverriding())
                .as("method is detected as overriding").isTrue();
    }

    @Test
    public void isOverriding_returns_true_for_generic_implementation_of_generic_interface_method() {
        assertThat(importMethod(SomeClassImplementingGenericsGeneric.class, "genericParam", Object.class).isOverriding())
                .as("method is detected as overriding").isTrue();
        assertThat(importMethod(SomeClassImplementingGenericsConcrete.class, "genericParam", String.class).isOverriding())
                .as("method is detected as overriding").isTrue();
        assertThat(importMethod(SomeClassImplementingGenericsGeneric.class, "genericReturnType").isOverriding())
                .as("method is detected as overriding").isTrue();
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

    @SuppressWarnings("unused")
    private interface SomeGenericInterface<T, U> {

        void genericParam(T param);

        U genericReturnType();
    }

    private static class SomeClassImplementingGenericsConcrete implements SomeGenericInterface<String, File> {

        @Override
        public void genericParam(String param) {
        }

        @Override
        public File genericReturnType() {
            return null;
        }
    }

    private static class SomeClassImplementingGenericsGeneric<X extends String, Y extends File> implements SomeGenericInterface<X, Y> {

        @Override
        public void genericParam(X param) {
        }

        @Override
        public Y genericReturnType() {
            return null;
        }
    }
}
