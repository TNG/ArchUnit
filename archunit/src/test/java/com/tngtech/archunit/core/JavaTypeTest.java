package com.tngtech.archunit.core;

import java.io.Serializable;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.ArchUnitException.ReflectionException;
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
    public void asm_object_type() {
        JavaType objectType = JavaType.From.fromAsmObjectTypeName("java/lang/Object");

        assertThat(objectType).isEquivalentTo(Object.class);
    }

    @Test
    @UseDataProvider("primitives")
    public void primitive_types_by_name_and_descriptor(String name, Class<?> expected) {
        JavaType primitiveType = JavaType.From.name(name);

        assertThat(primitiveType).isEquivalentTo(expected);
    }

    @Test
    @UseDataProvider("arrays")
    public void array_types_by_name_and_canonical_name(String name, Class<?> expected) {
        JavaType arrayType = JavaType.From.name(name);

        assertThat(arrayType).isEquivalentTo(expected);
    }

    @Test
    public void object_name() {
        JavaType arrayType = JavaType.From.name(Object.class.getName());

        assertThat(arrayType).isEquivalentTo(Object.class);
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

    @DataProvider
    public static Object[][] primitives() {
        return ImmutableList.builder()
                .addAll(namesToPrimitive(void.class))
                .addAll(namesToPrimitive(boolean.class))
                .addAll(namesToPrimitive(byte.class))
                .addAll(namesToPrimitive(char.class))
                .addAll(namesToPrimitive(short.class))
                .addAll(namesToPrimitive(int.class))
                .addAll(namesToPrimitive(long.class))
                .addAll(namesToPrimitive(float.class))
                .addAll(namesToPrimitive(double.class))
                .build().toArray(new Object[0][]);
    }

    private static List<Object[]> namesToPrimitive(Class<?> primitiveType) {
        return ImmutableList.of(
                new Object[]{primitiveType.getName(), primitiveType},
                new Object[]{Type.getType(primitiveType).getDescriptor(), primitiveType});
    }

    @DataProvider
    public static Object[][] arrays() {
        return ImmutableList.builder()
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
                .build().toArray(new Object[0][]);
    }

    private static List<Object[]> namesToArray(Class<?> arrayType) {
        return ImmutableList.of(
                new Object[]{arrayType.getName(), arrayType},
                new Object[]{arrayType.getCanonicalName(), arrayType});
    }
}