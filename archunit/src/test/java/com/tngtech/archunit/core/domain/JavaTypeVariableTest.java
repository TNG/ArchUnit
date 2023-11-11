package com.tngtech.archunit.core.domain;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypeErasuresOf;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;

public class JavaTypeVariableTest {

    @Test
    public void type_variable_name() {
        @SuppressWarnings("unused")
        class ClassWithUnboundTypeParameter<SOME_NAME> {
        }

        JavaTypeVariable<JavaClass> type = new ClassFileImporter().importClass(ClassWithUnboundTypeParameter.class).getTypeParameters().get(0);

        assertThat(type.getName()).isEqualTo("SOME_NAME");
    }

    @Test
    public void type_variable_class_owner() {
        @SuppressWarnings("unused")
        class ClassWithUnboundTypeParameter<SOME_NAME> {
        }

        JavaClass javaClass = new ClassFileImporter().importClass(ClassWithUnboundTypeParameter.class);
        JavaTypeVariable<JavaClass> type = javaClass.getTypeParameters().get(0);

        assertThat(type.getOwner()).isSameAs(javaClass);
        assertThat(type.getGenericDeclaration()).isSameAs(javaClass);
    }

    @Test
    public void type_variable_upper_bounds() {
        @SuppressWarnings("unused")
        class ClassWithUnboundTypeParameter<T extends HashMap<Object, Object> & Serializable> {
        }

        JavaTypeVariable<JavaClass> type = new ClassFileImporter().importClass(ClassWithUnboundTypeParameter.class).getTypeParameters().get(0);

        assertThatTypeErasuresOf(type.getBounds()).matchExactly(HashMap.class, Serializable.class);
        assertThatTypeErasuresOf(type.getUpperBounds()).matchExactly(HashMap.class, Serializable.class);
    }

    @Test
    public void erased_unbound_type_variable_is_java_lang_Object() {
        @SuppressWarnings("unused")
        class ClassWithUnboundTypeParameter<T> {
        }

        JavaTypeVariable<JavaClass> type = new ClassFileImporter().importClass(ClassWithUnboundTypeParameter.class).getTypeParameters().get(0);

        assertThatType(type.toErasure()).matches(Object.class);
    }

    @Test
    public void erased_type_variable_bound_by_single_class_is_this_class() {
        @SuppressWarnings("unused")
        class ClassWithBoundTypeParameterWithSingleClassBound<T extends Serializable> {
        }

        JavaTypeVariable<JavaClass> type = new ClassFileImporter().importClass(ClassWithBoundTypeParameterWithSingleClassBound.class).getTypeParameters().get(0);

        assertThatType(type.toErasure()).matches(Serializable.class);
    }

    @Test
    public void erased_type_variable_bound_by_single_generic_class_is_the_erasure_of_this_class() {
        @SuppressWarnings("unused")
        class ClassWithBoundTypeParameterWithSingleGenericClassBound<T extends List<String>> {
        }

        JavaTypeVariable<JavaClass> type = new ClassFileImporter().importClass(ClassWithBoundTypeParameterWithSingleGenericClassBound.class).getTypeParameters().get(0);

        assertThatType(type.toErasure()).matches(List.class);
    }

    @Test
    public void erased_type_variable_bound_by_multiple_generic_classes_and_interfaces_is_the_erasure_of_the_leftmost_bound() {
        @SuppressWarnings("unused")
        class ClassWithBoundTypeParameterWithMultipleGenericClassAndInterfaceBounds<T extends HashMap<String, String> & Iterable<String> & Serializable> {
        }

        JavaTypeVariable<JavaClass> type = new ClassFileImporter().importClass(ClassWithBoundTypeParameterWithMultipleGenericClassAndInterfaceBounds.class).getTypeParameters().get(0);

        assertThatType(type.toErasure()).matches(HashMap.class);
    }

    @Test
    public void erased_type_variable_bound_by_concrete_array_type_is_array_type() {
        @SuppressWarnings("unused")
        class ClassWithBoundTypeParameterWithSingleGenericArrayBound<T extends List<Object[]>, U extends List<String[][]>, V extends List<List<?>[][][]>> {
        }

        List<JavaTypeVariable<JavaClass>> typeParameters = new ClassFileImporter().importClass(ClassWithBoundTypeParameterWithSingleGenericArrayBound.class).getTypeParameters();

        assertThatType(getTypeArgumentOfFirstBound(typeParameters.get(0)).toErasure()).matches(Object[].class);
        assertThatType(getTypeArgumentOfFirstBound(typeParameters.get(1)).toErasure()).matches(String[][].class);
        assertThatType(getTypeArgumentOfFirstBound(typeParameters.get(2)).toErasure()).matches(List[][][].class);
    }

    @Test
    public void erased_type_variable_bound_by_generic_array_type_is_array_with_erasure_component_type() {
        @SuppressWarnings("unused")
        class ClassWithBoundTypeParameterWithGenericArrayBounds<A, B extends String, C extends List<?>, T extends List<A[]>, U extends List<B[][]>, V extends List<C[][][]>> {
        }

        List<JavaTypeVariable<JavaClass>> typeParameters = new ClassFileImporter().importClass(ClassWithBoundTypeParameterWithGenericArrayBounds.class).getTypeParameters();

        assertThatType(getTypeArgumentOfFirstBound(typeParameters.get(3)).toErasure()).matches(Object[].class);
        assertThatType(getTypeArgumentOfFirstBound(typeParameters.get(4)).toErasure()).matches(String[][].class);
        assertThatType(getTypeArgumentOfFirstBound(typeParameters.get(5)).toErasure()).matches(List[][][].class);
    }

    @Test
    public void all_involved_raw_types() {
        class SampleClass<T extends String & List<Serializable[]>> {
            @SuppressWarnings("unused")
            private T field;
        }

        JavaTypeVariable<?> typeVariable = (JavaTypeVariable<?>) new ClassFileImporter().importClass(SampleClass.class).getField("field").getType();

        assertThatTypes(typeVariable.getAllInvolvedRawTypes()).matchInAnyOrder(String.class, List.class, Serializable.class);
    }

    @Test
    public void all_involved_raw_types_of_generic_array() {
        class SampleClass<T extends String & List<Serializable>> {
            @SuppressWarnings("unused")
            private T[][] field;
        }

        JavaGenericArrayType typeVariable = (JavaGenericArrayType) new ClassFileImporter().importClass(SampleClass.class).getField("field").getType();

        assertThatTypes(typeVariable.getAllInvolvedRawTypes()).matchInAnyOrder(String.class, List.class, Serializable.class);
    }

    @Test
    public void toString_unbounded() {
        @SuppressWarnings("unused")
        class Unbounded<NAME> {
        }

        JavaTypeVariable<JavaClass> typeVariable = new ClassFileImporter().importClass(Unbounded.class).getTypeParameters().get(0);

        assertThat(typeVariable.toString())
                .contains(JavaTypeVariable.class.getSimpleName())
                .contains("NAME")
                .doesNotContain("extends");
    }

    @Test
    public void toString_upper_bounded_by_single_bound() {
        @SuppressWarnings("unused")
        class BoundedBySingleBound<NAME extends String> {
        }

        JavaTypeVariable<JavaClass> typeVariable = new ClassFileImporter().importClass(BoundedBySingleBound.class).getTypeParameters().get(0);

        assertThat(typeVariable.toString())
                .contains(JavaTypeVariable.class.getSimpleName())
                .contains("NAME extends java.lang.String");
    }

    @Test
    public void toString_upper_bounded_by_multiple_bounds() {
        @SuppressWarnings("unused")
        class BoundedByMultipleBounds<NAME extends String & Serializable> {
        }

        JavaTypeVariable<JavaClass> typeVariable = new ClassFileImporter().importClass(BoundedByMultipleBounds.class).getTypeParameters().get(0);

        assertThat(typeVariable.toString())
                .contains(JavaTypeVariable.class.getSimpleName())
                .contains("NAME extends java.lang.String & java.io.Serializable");
    }

    private static JavaType getTypeArgumentOfFirstBound(JavaTypeVariable<JavaClass> typeParameter) {
        JavaParameterizedType firstBound = (JavaParameterizedType) typeParameter.getBounds().get(0);
        return firstBound.getActualTypeArguments().get(0);
    }
}
