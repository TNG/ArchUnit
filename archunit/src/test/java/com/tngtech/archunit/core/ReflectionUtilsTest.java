package com.tngtech.archunit.core;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.ReflectionUtils.Predicate;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class ReflectionUtilsTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

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
    public void getAllConstructors() {
        Collection<Constructor<?>> constructors = ReflectionUtils.getAllConstructors(Child.class, new Predicate<Constructor<?>>() {
            @Override
            public boolean apply(Constructor<?> input) {
                return input.getDeclaringClass() == Child.class || input.getDeclaringClass() == LowerMiddle.class;
            }
        });

        assertThat(constructors).containsOnly(
                constructor(Child.class),
                constructor(LowerMiddle.class)
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

    @DataProvider
    public static Object[][] primitives() {
        return new Object[][]{
                {void.class.getName(), void.class},
                {boolean.class.getName(), boolean.class},
                {byte.class.getName(), byte.class},
                {char.class.getName(), char.class},
                {short.class.getName(), short.class},
                {int.class.getName(), int.class},
                {long.class.getName(), long.class},
                {float.class.getName(), float.class},
                {double.class.getName(), double.class},
        };
    }

    @Test
    @UseDataProvider("primitives")
    public void classForName_resolves_primitive_type_names(String name, Class<?> expected) {
        assertThat(ReflectionUtils.classForName(name)).isEqualTo(expected);
    }

    @DataProvider
    public static Object[][] arrays() {
        return ImmutableList.builder()
                .addAll(namesTo(boolean[].class))
                .addAll(namesTo(byte[].class))
                .addAll(namesTo(char[].class))
                .addAll(namesTo(short[].class))
                .addAll(namesTo(int[].class))
                .addAll(namesTo(long[].class))
                .addAll(namesTo(float[].class))
                .addAll(namesTo(double[].class))
                .addAll(namesTo(Object[].class))
                .addAll(namesTo(boolean[][].class))
                .addAll(namesTo(byte[][].class))
                .addAll(namesTo(char[][].class))
                .addAll(namesTo(short[][].class))
                .addAll(namesTo(int[][].class))
                .addAll(namesTo(long[][].class))
                .addAll(namesTo(float[][].class))
                .addAll(namesTo(double[][].class))
                .addAll(namesTo(Object[][].class))
                .build().toArray(new Object[0][]);
    }

    private static List<Object[]> namesTo(Class<?> arrayType) {
        return ImmutableList.of(
                new Object[]{arrayType.getName(), arrayType},
                new Object[]{arrayType.getCanonicalName(), arrayType});
    }

    @Test
    @UseDataProvider("arrays")
    public void classForName_resolves_arrays_type_names(String name, Class<?> expected) {
        assertThat(ReflectionUtils.classForName(name)).isEqualTo(expected);
    }

    @Test
    public void classForName_if_name_is_a_standard_class_name() {
        assertThat(ReflectionUtils.classForName(getClass().getName())).isEqualTo(getClass());
    }

    @Test
    public void classForName_if_type_doesnt_exist() {
        thrown.expect(ReflectionException.class);
        ReflectionUtils.classForName("does.not.exist");
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