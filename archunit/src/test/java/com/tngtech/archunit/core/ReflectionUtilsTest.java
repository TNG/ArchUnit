package com.tngtech.archunit.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReflectionUtilsTest {
    @Test
    public void testGetAllFieldsSortedFromChildToParent() {
        List<Field> fields = ReflectionUtils.getAllFieldsSortedFromChildToParent(Child.class);

        assertThat(fields).containsExactly(
                field(Child.class, "field"),
                field(UpperMiddle.class, "field"),
                field(LowerMiddle.class, "field"),
                field(Parent.class, "field")
        );
    }

    @Test
    public void testGetAllConstructorsSortedFromChildToParent() {
        List<Constructor<?>> constructors = ReflectionUtils.getAllConstructorsSortedFromChildToParent(Child.class);

        assertThat(constructors).containsExactly(
                constructor(Child.class),
                constructor(UpperMiddle.class),
                constructor(LowerMiddle.class),
                constructor(Parent.class),
                constructor(Object.class)
        );
    }

    @Test
    public void testGetAllMethodsSortedFromChildToParent() {
        List<Method> methods = ReflectionUtils.getAllMethodsSortedFromChildToParent(Child.class);
        methods = FluentIterable.from(methods).filter(named("overrideMe")).toList();

        assertThat(methods).containsExactly(
                method(Child.class, "overrideMe"),
                method(UpperMiddle.class, "overrideMe"),
                method(LowerMiddle.class, "overrideMe"),
                method(Parent.class, "overrideMe")
        );
    }

    private Predicate<Method> named(final String name) {
        return new Predicate<Method>() {
            @Override
            public boolean apply(Method input) {
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

        public Parent() {
        }

        public void overrideMe() {
        }
    }

    private static class LowerMiddle extends Parent {
        private int field;

        public LowerMiddle() {
        }

        @Override
        public void overrideMe() {
        }
    }

    private static class UpperMiddle extends LowerMiddle {
        private int field;

        public UpperMiddle() {
        }

        @Override
        public void overrideMe() {
        }
    }

    private static class Child extends UpperMiddle {
        private int field;

        public Child() {
        }

        @Override
        public void overrideMe() {
        }
    }
}