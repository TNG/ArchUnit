package com.tngtech.archunit.core.importer;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaGenericArrayType;
import com.tngtech.archunit.core.domain.JavaParameterizedType;
import com.tngtech.archunit.core.domain.JavaType;
import com.tngtech.archunit.core.domain.JavaWildcardType;
import com.tngtech.archunit.core.importer.testexamples.typeannotations.ClassRetainedTypeUseAnnotation;
import com.tngtech.archunit.core.importer.testexamples.typeannotations.ClassWithTypeUseAnnotatedFields;
import com.tngtech.archunit.core.importer.testexamples.typeannotations.RuntimeRetainedFieldAndTypeUseAnnotation;
import com.tngtech.archunit.core.importer.testexamples.typeannotations.RuntimeRetainedTypeUseAnnotation;
import com.tngtech.archunit.core.importer.testexamples.typeannotations.SecondRuntimeRetainedTypeUseAnnotation;
import org.assertj.core.api.AbstractCollectionAssert;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.api.ObjectAssert;
import org.junit.Ignore;
import org.junit.Test;

import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static java.util.stream.Collectors.toSet;

/**
 * Tests for the JSR 308 annotation targets {@link java.lang.annotation.ElementType#TYPE_USE}
 * and {@link java.lang.annotation.ElementType#TYPE_PARAMETER} (Java 8+).
 * <p>
 * Premise: for {@link java.lang.annotation.RetentionPolicy#RUNTIME RUNTIME}-retained annotations
 * the ArchUnit model must agree with what the Java reflection API reports for the same element.
 * RUNTIME-retention tests therefore first assert the expected statement against
 * {@link java.lang.reflect.AnnotatedType} obtained via reflection (which also validates the
 * fixture layout on its own) and then assert the equivalent statement against the ArchUnit model.
 * {@link java.lang.annotation.RetentionPolicy#CLASS CLASS}-retained annotations are only visible
 * via bytecode, so those tests only assert against the ArchUnit model.
 * <p>
 * These annotation targets are not yet supported by ArchUnit's importer. The tests in this class
 * serve as an executable specification for the expected behavior once support is implemented and
 * the whole class is therefore currently {@link Ignore ignored}. Remove the class-level
 * {@code @Ignore} once support for these annotation targets is implemented.
 */
@Ignore("TYPE_USE / TYPE_PARAMETER annotations are not yet supported by the importer")
public class ClassFileImporterTypeAnnotationsTest {

    // ==== TYPE_USE on field / method / constructor ====

    @Test
    public void imports_runtime_retained_type_use_annotation_on_field_type() {
        String fieldName = "fieldWithRuntimeRetainedTypeUseAnnotationOnType";

        Field reflectedField = reflect(fieldName);
        JavaField field = importField(fieldName);
        // field itself
        assertThat(field.isAnnotatedWith(RuntimeRetainedTypeUseAnnotation.class))
                .as("@RuntimeRetainedTypeUseAnnotation is on (annotated) field type instead of field")
                .isFalse()
                .as("agrees with Java reflection")
                .isEqualTo(reflectedField.isAnnotationPresent(RuntimeRetainedTypeUseAnnotation.class));
        assertThat(field.isAnnotatedWith(SecondRuntimeRetainedTypeUseAnnotation.class))
                .as("@SecondRuntimeRetainedTypeUseAnnotation is on raw field type instead of field")
                .isFalse()
                .as("agrees with Java reflection")
                .isEqualTo(reflectedField.isAnnotationPresent(SecondRuntimeRetainedTypeUseAnnotation.class));
        // raw type of field. field.getRawType() == field.getType().toErasure()
        assertThat(field.getRawType().isAnnotatedWith(RuntimeRetainedTypeUseAnnotation.class))
                .as("@RuntimeRetainedTypeUseAnnotation is on field type instead of field")
                .isFalse()
                .as("agrees with Java reflection")
                .isEqualTo(reflectedField.getType().isAnnotationPresent(RuntimeRetainedTypeUseAnnotation.class));
        assertThat(field.getRawType().isAnnotatedWith(SecondRuntimeRetainedTypeUseAnnotation.class))
                .as("@SecondRuntimeRetainedTypeUseAnnotation is on raw field type instead of field")
                .isTrue()
                .as("agrees with Java reflection")
                .isEqualTo(reflectedField.getType().isAnnotationPresent(SecondRuntimeRetainedTypeUseAnnotation.class));
        // annotated type of field
        assertThat(field.getAnnotatedType().isAnnotatedWith(RuntimeRetainedTypeUseAnnotation.class))
                .as("@RuntimeRetainedTypeUseAnnotation is on field type instead of field")
                .isTrue()
                .as("agrees with Java reflection")
                .isEqualTo(reflectedField.getAnnotatedType().isAnnotationPresent(RuntimeRetainedTypeUseAnnotation.class));
        assertThat(field.getAnnotatedType().isAnnotatedWith(SecondRuntimeRetainedTypeUseAnnotation.class))
                .as("@SecondRuntimeRetainedTypeUseAnnotation is on raw field type instead of field")
                .isFalse()
                .as("agrees with Java reflection")
                .isEqualTo(reflectedField.getAnnotatedType().isAnnotationPresent(SecondRuntimeRetainedTypeUseAnnotation.class));

        // alternative shorter syntax:
        assertThatClassesOf(field.getAnnotations())
                .as("field itself is not annotated")
                .containsExactlyInAnyOrder()
                .as("agrees with reflection")
                .containsExactlyInAnyOrderElementsOf(classesOf(reflectedField.getAnnotations()));
        assertThatClassesOf(field.getRawType().getAnnotations())
                .as("raw type of field is annotated with SecondRuntimeRetainedTypeUseAnnotation")
                .containsExactlyInAnyOrder(SecondRuntimeRetainedTypeUseAnnotation.class)
                .as("agrees with reflection")
                .containsExactlyInAnyOrderElementsOf(classesOf(reflectedField.getType().getAnnotations()));
        assertThatClassesOf(field.getAnnotatedType().getAnnotations())
                .as("type of field is annotated with RuntimeRetainedTypeUseAnnotation")
                .containsExactlyInAnyOrder(RuntimeRetainedTypeUseAnnotation.class)
                .as("agrees with reflection")
                .containsExactlyInAnyOrderElementsOf(classesOf(reflectedField.getType().getAnnotations()));
    }

    private static Set<? extends Class<? extends Annotation>> classesOf(Annotation... annotations) {
        return Arrays.stream(annotations).map(Annotation::annotationType).collect(toSet());
    }

    private static AbstractCollectionAssert<?, Collection<? extends Class<? extends Annotation>>, Class<? extends Annotation>, ObjectAssert<Class<? extends Annotation>>> assertThatClassesOf(Set<? extends JavaAnnotation<?>> annotations) {
        @SuppressWarnings("unchecked")
        Class<Class<? extends Annotation>> elementType = (Class<Class<? extends Annotation>>) (Class<?>) Annotation.class;
        return assertThat(annotations)
                .map(JavaAnnotation::getRawType)
                .map(JavaClass::reflect)
                .asInstanceOf(InstanceOfAssertFactories.set(elementType));
    }

    @Test
    public void imports_class_retained_type_use_annotation_on_field_type() {
        JavaField field = importField("fieldWithClassRetainedTypeUseAnnotationOnType");

        assertThat(field.isAnnotatedWith(ClassRetainedTypeUseAnnotation.class))
                .as("field type carries @ClassRetainedTypeUseAnnotation")
                .isTrue();
    }

    @Test
    public void imports_type_use_annotation_on_generic_field_type_argument() {
        String fieldName = "fieldWithRuntimeRetainedTypeUseAnnotationOnTypeArgument";

        AnnotatedParameterizedType reflectedFieldType = (AnnotatedParameterizedType) reflect(fieldName).getAnnotatedType();
        AnnotatedType reflectedTypeArgument = reflectedFieldType.getAnnotatedActualTypeArguments()[0];
        assertThat(reflectedTypeArgument.isAnnotationPresent(RuntimeRetainedTypeUseAnnotation.class))
                .as("Java reflection reports @RuntimeRetainedTypeUseAnnotation on type argument")
                .isTrue();

        JavaField field = importField(fieldName);
        JavaType typeArgument = ((JavaParameterizedType) field.getType()).getActualTypeArguments().get(0);
        assertThat(hasAnnotationOfType(typeArgument, RuntimeRetainedTypeUseAnnotation.class))
                .as("ArchUnit agrees on @RuntimeRetainedTypeUseAnnotation on type argument")
                .isTrue();
    }

    @Test
    public void imports_type_use_annotation_on_nested_generic_field_type_argument() {
        String fieldName = "fieldWithRuntimeRetainedTypeUseAnnotationOnNestedTypeArgument";

        AnnotatedParameterizedType reflectedFieldType = (AnnotatedParameterizedType) reflect(fieldName).getAnnotatedType();
        AnnotatedParameterizedType reflectedOuterSecondArg =
                (AnnotatedParameterizedType) reflectedFieldType.getAnnotatedActualTypeArguments()[1];
        AnnotatedType reflectedNestedArg = reflectedOuterSecondArg.getAnnotatedActualTypeArguments()[0];
        assertThat(reflectedNestedArg.isAnnotationPresent(RuntimeRetainedTypeUseAnnotation.class))
                .as("Java reflection reports @RuntimeRetainedTypeUseAnnotation on nested type argument")
                .isTrue();

        JavaField field = importField(fieldName);
        JavaType outerSecondArg = ((JavaParameterizedType) field.getType()).getActualTypeArguments().get(1);
        JavaType nestedArg = ((JavaParameterizedType) outerSecondArg).getActualTypeArguments().get(0);
        assertThat(hasAnnotationOfType(nestedArg, RuntimeRetainedTypeUseAnnotation.class))
                .as("ArchUnit agrees on @RuntimeRetainedTypeUseAnnotation on nested type argument")
                .isTrue();
    }

    @Test
    public void imports_type_use_annotation_on_wildcard_upper_bound_in_field_type() {
        String fieldName = "fieldWithRuntimeRetainedTypeUseAnnotationOnWildcardUpperBound";

        AnnotatedParameterizedType reflectedFieldType = (AnnotatedParameterizedType) reflect(fieldName).getAnnotatedType();
        AnnotatedWildcardType reflectedWildcard = (AnnotatedWildcardType) reflectedFieldType.getAnnotatedActualTypeArguments()[0];
        AnnotatedType reflectedUpperBound = reflectedWildcard.getAnnotatedUpperBounds()[0];
        assertThat(reflectedUpperBound.isAnnotationPresent(RuntimeRetainedTypeUseAnnotation.class))
                .as("Java reflection reports @RuntimeRetainedTypeUseAnnotation on wildcard upper bound")
                .isTrue();

        JavaField field = importField(fieldName);
        JavaWildcardType wildcard = (JavaWildcardType) ((JavaParameterizedType) field.getType()).getActualTypeArguments().get(0);
        JavaType upperBound = wildcard.getUpperBounds().get(0);
        assertThat(hasAnnotationOfType(upperBound, RuntimeRetainedTypeUseAnnotation.class))
                .as("ArchUnit agrees on @RuntimeRetainedTypeUseAnnotation on wildcard upper bound")
                .isTrue();
    }

    @Test
    public void imports_type_use_annotation_on_wildcard_lower_bound_in_field_type() {
        String fieldName = "fieldWithRuntimeRetainedTypeUseAnnotationOnWildcardLowerBound";

        AnnotatedParameterizedType reflectedFieldType = (AnnotatedParameterizedType) reflect(fieldName).getAnnotatedType();
        AnnotatedWildcardType reflectedWildcard = (AnnotatedWildcardType) reflectedFieldType.getAnnotatedActualTypeArguments()[0];
        AnnotatedType reflectedLowerBound = reflectedWildcard.getAnnotatedLowerBounds()[0];
        assertThat(reflectedLowerBound.isAnnotationPresent(RuntimeRetainedTypeUseAnnotation.class))
                .as("Java reflection reports @RuntimeRetainedTypeUseAnnotation on wildcard lower bound")
                .isTrue();

        JavaField field = importField(fieldName);
        JavaWildcardType wildcard = (JavaWildcardType) ((JavaParameterizedType) field.getType()).getActualTypeArguments().get(0);
        JavaType lowerBound = wildcard.getLowerBounds().get(0);
        assertThat(hasAnnotationOfType(lowerBound, RuntimeRetainedTypeUseAnnotation.class))
                .as("ArchUnit agrees on @RuntimeRetainedTypeUseAnnotation on wildcard lower bound")
                .isTrue();
    }

    @Test
    public void imports_type_use_annotation_on_wildcard_in_field_type() {
        String fieldName = "fieldWithRuntimeRetainedTypeUseAnnotationOnWildcard";

        AnnotatedParameterizedType reflectedFieldType = (AnnotatedParameterizedType) reflect(fieldName).getAnnotatedType();
        AnnotatedType reflectedWildcard = reflectedFieldType.getAnnotatedActualTypeArguments()[0];
        assertThat(reflectedWildcard.isAnnotationPresent(RuntimeRetainedTypeUseAnnotation.class))
                .as("Java reflection reports @RuntimeRetainedTypeUseAnnotation on wildcard itself")
                .isTrue();

        JavaField field = importField(fieldName);
        JavaType wildcard = ((JavaParameterizedType) field.getType()).getActualTypeArguments().get(0);
        assertThat(hasAnnotationOfType(wildcard, RuntimeRetainedTypeUseAnnotation.class))
                .as("ArchUnit agrees on @RuntimeRetainedTypeUseAnnotation on wildcard itself")
                .isTrue();
    }

    @Test
    public void imports_type_use_annotation_on_array_element_type_of_field() {
        String fieldName = "fieldWithRuntimeRetainedTypeUseAnnotationOnArrayElementType";

        AnnotatedArrayType reflectedFieldType = (AnnotatedArrayType) reflect(fieldName).getAnnotatedType();
        AnnotatedType reflectedComponent = reflectedFieldType.getAnnotatedGenericComponentType();
        assertThat(reflectedComponent.isAnnotationPresent(RuntimeRetainedTypeUseAnnotation.class))
                .as("Java reflection reports @RuntimeRetainedTypeUseAnnotation on array element type")
                .isTrue();

        JavaField field = importField(fieldName);
        JavaType componentType = arrayComponentType(field.getType());
        assertThat(hasAnnotationOfType(componentType, RuntimeRetainedTypeUseAnnotation.class))
                .as("ArchUnit agrees on @RuntimeRetainedTypeUseAnnotation on array element type")
                .isTrue();
    }

    @Test
    public void imports_type_use_annotation_on_array_type_of_field() {
        String fieldName = "fieldWithRuntimeRetainedTypeUseAnnotationOnArrayType";

        AnnotatedType reflectedFieldType = reflect(fieldName).getAnnotatedType();
        assertThat(reflectedFieldType.isAnnotationPresent(RuntimeRetainedTypeUseAnnotation.class))
                .as("Java reflection reports @RuntimeRetainedTypeUseAnnotation on array type itself")
                .isTrue();

        JavaField field = importField(fieldName);
        assertThat(hasAnnotationOfType(field.getType(), RuntimeRetainedTypeUseAnnotation.class))
                .as("ArchUnit agrees on @RuntimeRetainedTypeUseAnnotation on array type itself")
                .isTrue();
    }

    @Test
    public void imports_type_use_annotation_on_inner_type_qualifier_in_field_type() {
        String fieldName = "fieldWithRuntimeRetainedTypeUseAnnotationOnInnerTypeQualifier";

        AnnotatedType reflectedFieldType = reflect(fieldName).getAnnotatedType();
        assertThat(reflectedFieldType.isAnnotationPresent(RuntimeRetainedTypeUseAnnotation.class))
                .as("Java reflection reports @RuntimeRetainedTypeUseAnnotation on qualified inner type")
                .isTrue();

        JavaField field = importField(fieldName);
        assertThat(hasAnnotationOfType(field.getType(), RuntimeRetainedTypeUseAnnotation.class))
                .as("ArchUnit agrees on @RuntimeRetainedTypeUseAnnotation on qualified inner type")
                .isTrue();
    }

    @Test
    public void imports_multiple_type_use_annotations_at_same_position() {
        String fieldName = "fieldWithMultipleTypeUseAnnotationsAtSamePosition";

        AnnotatedType reflectedFieldType = reflect(fieldName).getAnnotatedType();
        assertThat(reflectedFieldType.isAnnotationPresent(RuntimeRetainedTypeUseAnnotation.class))
                .as("Java reflection reports @RuntimeRetainedTypeUseAnnotation on field type")
                .isTrue();
        assertThat(reflectedFieldType.isAnnotationPresent(SecondRuntimeRetainedTypeUseAnnotation.class))
                .as("Java reflection reports @SecondRuntimeRetainedTypeUseAnnotation at the same position")
                .isTrue();

        JavaField field = importField(fieldName);
        assertThat(field.isAnnotatedWith(RuntimeRetainedTypeUseAnnotation.class))
                .as("ArchUnit agrees on @RuntimeRetainedTypeUseAnnotation on field type")
                .isTrue();
        assertThat(field.isAnnotatedWith(SecondRuntimeRetainedTypeUseAnnotation.class))
                .as("ArchUnit agrees on @SecondRuntimeRetainedTypeUseAnnotation at the same position")
                .isTrue();
    }

    @Test
    public void imports_type_use_annotations_at_multiple_positions_of_same_field() {
        String fieldName = "fieldWithTypeUseAnnotationsAtMultiplePositions";

        AnnotatedArrayType reflectedFieldType = (AnnotatedArrayType) reflect(fieldName).getAnnotatedType();
        assertThat(reflectedFieldType.isAnnotationPresent(SecondRuntimeRetainedTypeUseAnnotation.class))
                .as("Java reflection reports @SecondRuntimeRetainedTypeUseAnnotation on array type")
                .isTrue();
        AnnotatedParameterizedType reflectedElementType =
                (AnnotatedParameterizedType) reflectedFieldType.getAnnotatedGenericComponentType();
        AnnotatedType reflectedTypeArgument = reflectedElementType.getAnnotatedActualTypeArguments()[0];
        assertThat(reflectedTypeArgument.isAnnotationPresent(RuntimeRetainedTypeUseAnnotation.class))
                .as("Java reflection reports @RuntimeRetainedTypeUseAnnotation on type argument of array element type")
                .isTrue();

        JavaField field = importField(fieldName);
        assertThat(hasAnnotationOfType(field.getType(), SecondRuntimeRetainedTypeUseAnnotation.class))
                .as("ArchUnit agrees on @SecondRuntimeRetainedTypeUseAnnotation on array type")
                .isTrue();
        JavaType elementType = arrayComponentType(field.getType());
        JavaType typeArgumentOfElement = ((JavaParameterizedType) elementType).getActualTypeArguments().get(0);
        assertThat(hasAnnotationOfType(typeArgumentOfElement, RuntimeRetainedTypeUseAnnotation.class))
                .as("ArchUnit agrees on @RuntimeRetainedTypeUseAnnotation on type argument of array element type")
                .isTrue();
    }

    // ==== retention and disambiguation ====

    @Test
    public void distinguishes_declaration_annotation_and_type_use_annotation_on_same_field() {
        String fieldName = "fieldWithBothDeclarationAndTypeUseAnnotation";

        Field reflectedField = reflect(fieldName);
        assertThat(reflectedField.isAnnotationPresent(RuntimeRetainedFieldAndTypeUseAnnotation.class))
                .as("Java reflection reports @RuntimeRetainedFieldAndTypeUseAnnotation as declaration annotation")
                .isTrue();
        assertThat(reflectedField.getAnnotatedType().isAnnotationPresent(RuntimeRetainedFieldAndTypeUseAnnotation.class))
                .as("Java reflection reports @RuntimeRetainedFieldAndTypeUseAnnotation as type annotation")
                .isTrue();

        JavaField field = importField(fieldName);
        assertThat(field.isAnnotatedWith(RuntimeRetainedFieldAndTypeUseAnnotation.class))
                .as("ArchUnit agrees on @RuntimeRetainedFieldAndTypeUseAnnotation as declaration annotation")
                .isTrue();
        assertThat(hasAnnotationOfType(field.getType(), RuntimeRetainedFieldAndTypeUseAnnotation.class))
                .as("ArchUnit agrees on @RuntimeRetainedFieldAndTypeUseAnnotation as type annotation")
                .isTrue();
    }

    private static JavaField importField(String fieldName) {
        return new ClassFileImporter().importPackagesOf(ClassWithTypeUseAnnotatedFields.class)
                .get(ClassWithTypeUseAnnotatedFields.class)
                .getField(fieldName);
    }

    private static Field reflect(String fieldName) {
        try {
            return ClassWithTypeUseAnnotatedFields.class.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Returns the component type of an array-typed {@link JavaType}, preserving generics for
     * generic array types (e.g. {@code List<String>[]}, whose model is a {@link JavaGenericArrayType}).
     * For raw arrays (whose model is a {@link JavaClass}) the plain {@link JavaClass#getComponentType()}
     * is used.
     */
    private static JavaType arrayComponentType(JavaType type) {
        if (type instanceof JavaGenericArrayType) {
            return ((JavaGenericArrayType) type).getComponentType();
        }
        return ((JavaClass) type).getComponentType();
    }

    /**
     * Placeholder helper for asserting a TYPE_USE annotation on a {@link JavaType} (e.g. on a
     * type argument, wildcard bound, array component type, ...). Once the importer surfaces
     * type annotations on {@link JavaType} directly (e.g. via a {@code getAnnotations()} method),
     * this helper can be replaced by a direct call on the model.
     */
    private static boolean hasAnnotationOfType(JavaType type, Class<? extends Annotation> annotationType) {
        for (JavaAnnotation<?> annotation : getTypeAnnotations(type)) {
            if (annotation.getRawType().isEquivalentTo(annotationType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Placeholder: no API to read type annotations off a {@link JavaType} exists yet. Once the
     * importer supports TYPE_USE annotations, this should be replaced with the real accessor
     * (e.g. {@code type.getAnnotations()}). Until then the tests using it are guarded by the
     * class-level {@link Ignore}.
     */
    private static Set<? extends JavaAnnotation<?>> getTypeAnnotations(JavaType type) {
        throw new UnsupportedOperationException(
                "Reading type annotations off a JavaType is not yet supported by the importer");
    }
}
