package com.tngtech.archunit.junit.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.function.Predicate;

import org.junit.Test;

import static com.tngtech.archunit.base.Predicates.alwaysTrue;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.field;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.method;
import static org.assertj.core.api.Assertions.assertThat;

public class ReflectionUtilsTest {
    @Test
    public void getAllFields() {
        Collection<Field> fields = ReflectionUtils.getAllFields(Child.class, named("field"));

        assertThat(fields).containsOnly(
                field(Child.class, "field"),
                field(UpperMiddle.class, "field"),
                field(LowerMiddle.class, "field"),
                field(Parent.class, "field")
        );
    }

    @Test
    public void getAllMethods() {
        Collection<Method> methods = ReflectionUtils.getAllMethods(Child.class, named("overrideMe"));

        assertThat(methods).containsOnly(
                method(Child.class, "overrideMe"),
                method(UpperMiddle.class, "overrideMe"),
                method(LowerMiddle.class, "overrideMe"),
                method(Parent.class, "overrideMe")
        );
    }

    @Test
    public void getAllMethods_of_interface() {
        assertThat(ReflectionUtils.getAllMethods(Subinterface.class, alwaysTrue()))
                .containsOnly(
                        method(SomeInterface.class, "foo"),
                        method(OtherInterface.class, "bar"));
    }

    @Test
    public void getAllFields_of_interface() {
        assertThat(ReflectionUtils.getAllFields(Subinterface.class, alwaysTrue()))
                .containsOnly(
                        field(SomeInterface.class, "SOME_CONSTANT"),
                        field(OtherInterface.class, "OTHER_CONSTANT"));
    }

    private Predicate<Member> named(String name) {
        return input -> input.getName().equals(name);
    }

    @SuppressWarnings("unused")
    private static class Parent {
        private int field;
        private String other;

        public Parent() {
        }

        void overrideMe() {
        }

        String someMethod(int param) {
            return null;
        }
    }

    @SuppressWarnings("unused")
    private static class LowerMiddle extends Parent implements SomeInterface {
        private int field;

        public LowerMiddle() {
        }

        @Override
        public void overrideMe() {
        }

        @Override
        public void foo() {
        }
    }

    @SuppressWarnings("unused")
    private static class UpperMiddle extends LowerMiddle implements OtherInterface {
        private int field;
        private String other;

        public UpperMiddle() {
        }

        @Override
        public void overrideMe() {
        }

        @Override
        String someMethod(int param) {
            return null;
        }

        @Override
        public void bar() {
        }
    }

    @SuppressWarnings("unused")
    private static class Child extends UpperMiddle implements ChildInterface {
        private int field;
        private String another;

        public Child() {
        }

        @Override
        public void overrideMe() {
        }

        @Override
        public void child() {
        }
    }

    private interface ChildInterface {
        void child();
    }

    @SuppressWarnings("unused")
    private interface SomeInterface {
        String SOME_CONSTANT = "SOME";

        void foo();
    }

    @SuppressWarnings("unused")
    private interface OtherInterface {
        String OTHER_CONSTANT = "OTHER";

        void bar();
    }

    private interface Subinterface extends SomeInterface, OtherInterface {
    }
}
