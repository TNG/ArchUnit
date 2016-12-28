package com.tngtech.archunit.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collection;

import com.tngtech.archunit.core.ReflectionUtils.Predicate;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
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
    public void getAllSupertypes() {
        assertThat(ReflectionUtils.getAllSuperTypes(Child.class)).containsOnly(
                Child.class, ChildInterface.class, UpperMiddle.class, LowerMiddle.class, Parent.class,
                SomeInterface.class, OtherInterface.class, Object.class
        );
    }

    @Test
    public void getAllMethods_of_interface() {
        assertThat(ReflectionUtils.getAllMethods(SubInterface.class, always(true)))
                .containsOnly(
                        method(SomeInterface.class, "foo"),
                        method(OtherInterface.class, "bar"));
    }

    private <T> Predicate<T> always(final boolean bool) {
        return new Predicate<T>() {
            @Override
            public boolean apply(T input) {
                return bool;
            }
        };
    }

    @Test
    public void getAllFields_of_interface() {
        assertThat(ReflectionUtils.getAllFields(SubInterface.class, always(true)))
                .containsOnly(
                        field(SomeInterface.class, "SOME_CONSTANT"),
                        field(OtherInterface.class, "OTHER_CONSTANT"));
    }

    private Predicate<Member> named(final String name) {
        return new Predicate<Member>() {
            @Override
            public boolean apply(Member input) {
                return input.getName().equals(name);
            }
        };
    }

    public static Field field(Class<?> clazz, String fieldName) {
        try {
            return clazz.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
    }

    public static Constructor<?> constructor(Class<?> clazz) {
        try {
            return clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public static Method method(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

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

    private interface SomeInterface {
        String SOME_CONSTANT = "SOME";

        void foo();
    }

    private interface OtherInterface {
        String OTHER_CONSTANT = "OTHER";

        void bar();
    }

    private interface SubInterface extends SomeInterface, OtherInterface {
    }
}