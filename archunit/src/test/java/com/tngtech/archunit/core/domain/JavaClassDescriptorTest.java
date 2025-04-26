package com.tngtech.archunit.core.domain;

import java.io.Serializable;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.base.ArchUnitException.ReflectionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.objectweb.asm.Type;

import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.DataProviders.$;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JavaClassDescriptorTest {

    @ParameterizedTest
    @MethodSource("primitives")
    void primitive_types_by_name_and_descriptor(String name, Class<?> expected) {
        JavaClassDescriptor primitiveType = JavaClassDescriptor.From.name(name);
        assertThat(primitiveType.isPrimitive()).isTrue();
        assertThat(primitiveType.isArray()).isFalse();
        assertThat(primitiveType.tryGetComponentType()).isEmpty();

        assertThat(primitiveType).isEquivalentTo(expected);
    }

    @ParameterizedTest
    @MethodSource("arrays")
    void array_types_by_name_and_canonical_name(String name, Class<?> expected) {
        JavaClassDescriptor arrayType = JavaClassDescriptor.From.name(name);
        assertThat(arrayType.isPrimitive()).isFalse();
        assertThat(arrayType.isArray()).isTrue();
        assertThat(arrayType.tryGetComponentType()).contains(JavaClassDescriptor.From.name(expected.getComponentType().getName()));

        assertThat(arrayType).isEquivalentTo(expected);
    }

    @Test
    public void object_name() {
        JavaClassDescriptor objectType = JavaClassDescriptor.From.name(Object.class.getName());
        assertThat(objectType.isPrimitive()).isFalse();
        assertThat(objectType.isArray()).isFalse();
        assertThat(objectType.tryGetComponentType()).isEmpty();

        assertThat(objectType).isEquivalentTo(Object.class);
    }

    @ParameterizedTest
    @MethodSource(value = "primitives")
    void resolves_primitive_type_names(String name, Class<?> expected) {
        assertThat(JavaClassDescriptor.From.name(name).resolveClass()).isEqualTo(expected);
    }

    @ParameterizedTest
    @MethodSource(value = "arrays")
    void resolves_arrays_type_names(String name, Class<?> expected) {
        assertThat(JavaClassDescriptor.From.name(name).resolveClass()).isEqualTo(expected);
    }

    @Test
    public void resolves_standard_class_name() {
        assertThat(JavaClassDescriptor.From.name(getClass().getName()).resolveClass()).isEqualTo(getClass());
    }

    @Test
    public void resolving_throws_exception_if_type_doesnt_exist() {
        assertThatThrownBy(
                () -> JavaClassDescriptor.From.name("does.not.exist").resolveClass()
        ).isInstanceOf(ReflectionException.class);
    }

    @Test
    public void anonymous_type() {
        Serializable input = new Serializable() {
        };

        JavaClassDescriptor anonymousType = JavaClassDescriptor.From.name(input.getClass().getName());

        assertThat(anonymousType.getFullyQualifiedClassName()).isEqualTo(getClass().getName() + "$1");
        assertThat(anonymousType.getSimpleClassName()).isEmpty();
        assertThat(anonymousType.getPackageName()).isEqualTo(getClass().getPackage().getName());
    }

    @Test
    public void special_chars_type() {
        JavaClassDescriptor specialChars = JavaClassDescriptor.From.name("s_123_wéirdâ.Weird_αρετη_Type");

        assertThat(specialChars.getFullyQualifiedClassName()).isEqualTo("s_123_wéirdâ.Weird_αρετη_Type");
        assertThat(specialChars.getSimpleClassName()).isEqualTo("Weird_αρετη_Type");
        assertThat(specialChars.getPackageName()).isEqualTo("s_123_wéirdâ");
    }

    @Test
    public void default_package() {
        JavaClassDescriptor specialChars = JavaClassDescriptor.From.name("DefaultPackage");

        assertThat(specialChars.getFullyQualifiedClassName()).isEqualTo("DefaultPackage");
        assertThat(specialChars.getSimpleClassName()).isEqualTo("DefaultPackage");
        assertThat(specialChars.getPackageName()).isEmpty();
    }

    static List<Arguments> primitives() {
        return ImmutableList.<Arguments>builder()
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

    @Test
    public void convert_object_descriptor_to_array_descriptor() {
        JavaClassDescriptor arrayDescriptor = JavaClassDescriptor.From.name(Object.class.getName()).toArrayDescriptor();

        assertThat(arrayDescriptor.getFullyQualifiedClassName()).isEqualTo(Object[].class.getName());
        assertThat(arrayDescriptor.getSimpleClassName()).isEqualTo(Object[].class.getSimpleName());
        assertThat(arrayDescriptor.getPackageName()).isEqualTo(Object.class.getPackage().getName());
    }

    @Test
    public void convert_primitive_descriptor_to_array_descriptor() {
        JavaClassDescriptor arrayDescriptor = JavaClassDescriptor.From.name(int.class.getName()).toArrayDescriptor();

        assertThat(arrayDescriptor.getFullyQualifiedClassName()).isEqualTo(int[].class.getName());
        assertThat(arrayDescriptor.getSimpleClassName()).isEqualTo(int[].class.getSimpleName());
        assertThat(arrayDescriptor.getPackageName()).isEqualTo("java.lang");
    }

    @Test
    public void convert_array_descriptor_to_2_dim_array_descriptor() {
        JavaClassDescriptor arrayDescriptor = JavaClassDescriptor.From.name(Object[].class.getName()).toArrayDescriptor();

        assertThat(arrayDescriptor.getFullyQualifiedClassName()).isEqualTo(Object[][].class.getName());
        assertThat(arrayDescriptor.getSimpleClassName()).isEqualTo(Object[][].class.getSimpleName());
        assertThat(arrayDescriptor.getPackageName()).isEqualTo(Object.class.getPackage().getName());
    }

    @Test
    public void converts_descriptor_repeatedly_multi_dim_array_descriptor() {
        JavaClassDescriptor arrayDescriptor = JavaClassDescriptor.From.name(Object.class.getName())
                .toArrayDescriptor().toArrayDescriptor().toArrayDescriptor();

        assertThat(arrayDescriptor.getFullyQualifiedClassName()).isEqualTo(Object[][][].class.getName());
        assertThat(arrayDescriptor.getSimpleClassName()).isEqualTo(Object[][][].class.getSimpleName());
        assertThat(arrayDescriptor.getPackageName()).isEqualTo(Object.class.getPackage().getName());
    }

    private static List<Arguments> namesToPrimitive(Class<?> primitiveType) {
        return ImmutableList.of(
                $(primitiveType.getName(), primitiveType),
                $(Type.getType(primitiveType).getDescriptor(), primitiveType));
    }

    static List<Arguments> arrays() {
        return ImmutableList.<Arguments>builder()
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

    private static List<Arguments> namesToArray(Class<?> arrayType) {
        return ImmutableList.of(
                $(arrayType.getName(), arrayType),
                $(arrayType.getCanonicalName(), arrayType));
    }
}
