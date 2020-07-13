package com.tngtech.archunit.core.domain;

import java.io.Serializable;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.base.ArchUnitException.ReflectionException;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.objectweb.asm.Type;

import static com.tngtech.archunit.testutil.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class JavaClassDescriptorTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    @UseDataProvider("primitives")
    public void primitive_types_by_name_and_descriptor(String name, Class<?> expected) {
        JavaClassDescriptor primitiveType = JavaClassDescriptor.From.name(name);
        assertThat(primitiveType.isPrimitive()).isTrue();
        assertThat(primitiveType.isArray()).isFalse();
        assertThat(primitiveType.tryGetComponentType()).isAbsent();

        assertThat(primitiveType).isEquivalentTo(expected);
    }

    @Test
    @UseDataProvider("arrays")
    public void array_types_by_name_and_canonical_name(String name, Class<?> expected) {
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
        assertThat(objectType.tryGetComponentType()).isAbsent();

        assertThat(objectType).isEquivalentTo(Object.class);
    }

    @Test
    @UseDataProvider(value = "primitives")
    public void resolves_primitive_type_names(String name, Class<?> expected) {
        assertThat(JavaClassDescriptor.From.name(name).resolveClass()).isEqualTo(expected);
    }

    @Test
    @UseDataProvider(value = "arrays")
    public void resolves_arrays_type_names(String name, Class<?> expected) {
        assertThat(JavaClassDescriptor.From.name(name).resolveClass()).isEqualTo(expected);
    }

    @Test
    public void resolves_standard_class_name() {
        assertThat(JavaClassDescriptor.From.name(getClass().getName()).resolveClass()).isEqualTo(getClass());
    }

    @Test
    public void resolving_throws_exception_if_type_doesnt_exist() {
        thrown.expect(ReflectionException.class);
        JavaClassDescriptor.From.name("does.not.exist").resolveClass();
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
        assertThat(arrayDescriptor.getPackageName()).isEmpty();
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
