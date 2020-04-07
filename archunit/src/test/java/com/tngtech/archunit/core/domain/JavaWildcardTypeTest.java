package com.tngtech.archunit.core.domain;

import java.io.Serializable;
import java.util.List;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.Assertions.assertThatTypes;

public class JavaWildcardTypeTest {

    @Test
    public void wildcard_name() {
        @SuppressWarnings("unused")
        class ClassWithUnboundTypeParameter<T extends List<?>> {
        }

        JavaWildcardType type = importWildcardTypeOf(ClassWithUnboundTypeParameter.class);

        assertThat(type.getName()).isEqualTo("?");
    }

    @Test
    public void wildcard_upper_bounds() {
        @SuppressWarnings("unused")
        class ClassWithUnboundTypeParameter<T extends List<? extends Serializable>> {
        }

        JavaWildcardType type = importWildcardTypeOf(ClassWithUnboundTypeParameter.class);

        assertThatTypes(type.getUpperBounds()).matchExactly(Serializable.class);
    }

    @Test
    public void wildcard_lower_bounds() {
        @SuppressWarnings("unused")
        class ClassWithUnboundTypeParameter<T extends List<? super Serializable>> {
        }

        JavaWildcardType type = importWildcardTypeOf(ClassWithUnboundTypeParameter.class);

        assertThatTypes(type.getLowerBounds()).matchExactly(Serializable.class);
    }

    @Test
    public void erased_unbound_wildcard_is_java_lang_Object() {
        @SuppressWarnings("unused")
        class ClassWithUnboundWildcard<T extends List<?>> {
        }

        JavaWildcardType type = importWildcardTypeOf(ClassWithUnboundWildcard.class);

        assertThatType(type.toErasure()).matches(Object.class);
    }

    @Test
    public void erased_wildcard_bound_by_single_class_is_this_class() {
        @SuppressWarnings("unused")
        class ClassWithBoundTypeParameterWithSingleClassBound<T extends List<? extends Serializable>> {
        }

        JavaWildcardType type = importWildcardTypeOf(ClassWithBoundTypeParameterWithSingleClassBound.class);

        assertThatType(type.toErasure()).matches(Serializable.class);
    }

    @Test
    public void erased_wildcard_bound_by_single_generic_class_is_the_erasure_of_this_class() {
        @SuppressWarnings("unused")
        class ClassWithBoundTypeParameterWithSingleGenericClassBound<T extends List<? extends List<String>>> {
        }

        JavaWildcardType type = importWildcardTypeOf(ClassWithBoundTypeParameterWithSingleGenericClassBound.class);

        assertThatType(type.toErasure()).matches(List.class);
    }

    @Test
    public void toString_unbounded() {
        @SuppressWarnings("unused")
        class Unbounded<T extends List<?>> {
        }

        JavaWildcardType wildcardType = importWildcardTypeOf(Unbounded.class);

        assertThat(wildcardType.toString())
                .contains(JavaWildcardType.class.getSimpleName())
                .contains("?")
                .doesNotContain("extends")
                .doesNotContain("super");
    }

    @Test
    public void toString_upper_bounded() {
        @SuppressWarnings("unused")
        class UpperBounded<T extends List<? extends String>> {
        }

        JavaWildcardType wildcardType = importWildcardTypeOf(UpperBounded.class);

        assertThat(wildcardType.toString())
                .contains(JavaWildcardType.class.getSimpleName())
                .contains("? extends java.lang.String")
                .doesNotContain("super");
    }

    @Test
    public void toString_lower_bounded() {
        @SuppressWarnings("unused")
        class LowerBounded<T extends List<? super String>> {
        }

        JavaWildcardType wildcardType = importWildcardTypeOf(LowerBounded.class);

        assertThat(wildcardType.toString())
                .contains(JavaWildcardType.class.getSimpleName())
                .contains("? super java.lang.String")
                .doesNotContain("extends");
    }

    private JavaWildcardType importWildcardTypeOf(Class<?> clazz) {
        JavaType listType = new ClassFileImporter().importClass(clazz).getTypeParameters().get(0)
                .getUpperBounds().get(0);
        JavaType wildcardType = ((JavaParameterizedType) listType).getActualTypeArguments().get(0);
        return (JavaWildcardType) wildcardType;
    }
}
