package com.tngtech.archunit.core;

import java.io.Serializable;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.base.ArchUnitException.ReflectionException;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.assertj.core.internal.cglib.asm.Type;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class JavaTypeTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    @UseDataProvider("primitives")
    public void primitive_types_by_name_and_descriptor(String name, Class<?> expected) {
        JavaType primitiveType = JavaType.From.name(name);
        assertThat(primitiveType.isPrimitive()).isTrue();
        assertThat(primitiveType.isArray()).isFalse();
        assertThat(primitiveType.tryGetComponentType()).isAbsent();

        assertThat(primitiveType).isEquivalentTo(expected);
    }

    @Test
    @UseDataProvider("arrays")
    public void array_types_by_name_and_canonical_name(String name, Class<?> expected) {
        JavaType arrayType = JavaType.From.name(name);
        assertThat(arrayType.isPrimitive()).isFalse();
        assertThat(arrayType.isArray()).isTrue();
        assertThat(arrayType.tryGetComponentType()).contains(JavaType.From.name(expected.getComponentType().getName()));

        assertThat(arrayType).isEquivalentTo(expected);
    }

    @Test
    public void object_name() {
        JavaType objectType = JavaType.From.name(Object.class.getName());
        assertThat(objectType.isPrimitive()).isFalse();
        assertThat(objectType.isArray()).isFalse();
        assertThat(objectType.tryGetComponentType()).isAbsent();

        assertThat(objectType).isEquivalentTo(Object.class);
    }

    @Test
    public void object_descriptor() {
        JavaType arrayType = JavaType.From.name(Type.getType(Object.class).getDescriptor());

        assertThat(arrayType).isEquivalentTo(Object.class);
    }

    @Test
    @UseDataProvider(value = "primitives")
    public void resolves_primitive_type_names(String name, Class<?> expected) {
        assertThat(JavaType.From.name(name).resolveClass()).isEqualTo(expected);
    }

    @Test
    @UseDataProvider(value = "arrays")
    public void resolves_arrays_type_names(String name, Class<?> expected) {
        assertThat(JavaType.From.name(name).resolveClass()).isEqualTo(expected);
    }

    @Test
    public void resolves_standard_class_name() {
        assertThat(JavaType.From.name(getClass().getName()).resolveClass()).isEqualTo(getClass());
    }

    @Test
    public void resolving_throws_exception_if_type_doesnt_exist() {
        thrown.expect(ReflectionException.class);
        JavaType.From.name("does.not.exist").resolveClass();
    }

    @Test
    public void anonymous_type() {
        JavaType anonymousType = JavaType.From.name(new Serializable() {}.getClass().getName());

        assertThat(anonymousType.getName()).isEqualTo(getClass().getName() + "$1");
        assertThat(anonymousType.getSimpleName()).isEmpty();
        assertThat(anonymousType.getPackage()).isEqualTo(getClass().getPackage().getName());
    }

    @Test
    public void special_chars_type() {
        JavaType specialChars = JavaType.From.name("s_123_wéirdâ.Weird_αρετη_Type");

        assertThat(specialChars.getName()).isEqualTo("s_123_wéirdâ.Weird_αρετη_Type");
        assertThat(specialChars.getSimpleName()).isEqualTo("Weird_αρετη_Type");
        assertThat(specialChars.getPackage()).isEqualTo("s_123_wéirdâ");
    }

    @Test
    public void default_package() {
        JavaType specialChars = JavaType.From.name("DefaultPackage");

        assertThat(specialChars.getName()).isEqualTo("DefaultPackage");
        assertThat(specialChars.getSimpleName()).isEqualTo("DefaultPackage");
        assertThat(specialChars.getPackage()).isEmpty();
    }

    @DataProvider
    public static List<List<Object>> primitives() {
        return ImmutableList.<List<Object>>builder()
                .addAll(namesToPrimitive(void.class))
                .addAll(namesToPrimitive(boolean.class))
                .addAll(namesToPrimitive(byte.class))
                .addAll(namesToPrimitive(char.class))
                .addAll(namesToPrimitive(short.class))
                .addAll(namesToPrimitive(int.class))
                .addAll(namesToPrimitive(long.class))
                .addAll(namesToPrimitive(float.class))
                .addAll(namesToPrimitive(double.class))
                .build();
    }

    private static List<List<Object>> namesToPrimitive(Class<?> primitiveType) {
        return ImmutableList.<List<Object>>of(
                ImmutableList.<Object>of(primitiveType.getName(), primitiveType),
                ImmutableList.<Object>of(Type.getType(primitiveType).getDescriptor(), primitiveType));
    }

    @DataProvider
    public static List<List<Object>> arrays() {
        return ImmutableList.<List<Object>>builder()
                .addAll(namesToArray(boolean[].class))
                .addAll(namesToArray(byte[].class))
                .addAll(namesToArray(char[].class))
                .addAll(namesToArray(short[].class))
                .addAll(namesToArray(int[].class))
                .addAll(namesToArray(long[].class))
                .addAll(namesToArray(float[].class))
                .addAll(namesToArray(double[].class))
                .addAll(namesToArray(Object[].class))
                .addAll(namesToArray(boolean[][].class))
                .addAll(namesToArray(byte[][].class))
                .addAll(namesToArray(char[][].class))
                .addAll(namesToArray(short[][].class))
                .addAll(namesToArray(int[][].class))
                .addAll(namesToArray(long[][].class))
                .addAll(namesToArray(float[][].class))
                .addAll(namesToArray(double[][].class))
                .addAll(namesToArray(Object[][].class))
                .build();
    }

    private static List<List<Object>> namesToArray(Class<?> arrayType) {
        return ImmutableList.<List<Object>>of(
                ImmutableList.<Object>of(arrayType.getName(), arrayType),
                ImmutableList.<Object>of(arrayType.getCanonicalName(), arrayType));
    }
}