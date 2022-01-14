package com.tngtech.archunit.core.importer;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.importer.testexamples.annotatedclassimport.ClassAnnotationWithArrays;
import com.tngtech.archunit.core.importer.testexamples.annotatedclassimport.ClassWithAnnotationWithEmptyArrays;
import com.tngtech.archunit.core.importer.testexamples.annotatedclassimport.ClassWithComplexAnnotations;
import com.tngtech.archunit.core.importer.testexamples.annotatedclassimport.ClassWithOneAnnotation;
import com.tngtech.archunit.core.importer.testexamples.annotatedclassimport.SimpleAnnotation;
import com.tngtech.archunit.core.importer.testexamples.annotatedclassimport.TypeAnnotationWithEnumAndArrayValue;
import com.tngtech.archunit.core.importer.testexamples.annotatedparameters.ClassWithMethodWithAnnotatedParameters;
import com.tngtech.archunit.core.importer.testexamples.annotationfieldimport.ClassWithAnnotatedFields;
import com.tngtech.archunit.core.importer.testexamples.annotationfieldimport.FieldAnnotationWithArrays;
import com.tngtech.archunit.core.importer.testexamples.annotationmethodimport.ClassWithAnnotatedMethods;
import com.tngtech.archunit.core.importer.testexamples.annotationmethodimport.MethodAnnotationWithArrays;
import com.tngtech.archunit.core.importer.testexamples.simpleimport.AnnotationParameter;
import com.tngtech.archunit.core.importer.testexamples.simpleimport.AnnotationToImport;
import com.tngtech.archunit.core.importer.testexamples.simpleimport.EnumToImport;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.importer.testexamples.SomeEnum.OTHER_VALUE;
import static com.tngtech.archunit.core.importer.testexamples.SomeEnum.SOME_VALUE;
import static com.tngtech.archunit.core.importer.testexamples.annotationmethodimport.ClassWithAnnotatedMethods.enumAndArrayAnnotatedMethod;
import static com.tngtech.archunit.core.importer.testexamples.annotationmethodimport.ClassWithAnnotatedMethods.methodAnnotatedWithEmptyArrays;
import static com.tngtech.archunit.core.importer.testexamples.annotationmethodimport.ClassWithAnnotatedMethods.stringAndIntAnnotatedMethod;
import static com.tngtech.archunit.core.importer.testexamples.annotationmethodimport.ClassWithAnnotatedMethods.stringAnnotatedMethod;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatAnnotation;
import static com.tngtech.archunit.testutil.Assertions.assertThatAnnotations;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;

@RunWith(DataProviderRunner.class)
public class ClassFileImporterAnnotationsTest {

    @Test
    public void imports_simple_annotation() {
        JavaClass javaClass = new ClassFileImporter().importPackagesOf(AnnotationToImport.class).get(AnnotationToImport.class);

        assertThat(javaClass)
                .matches(AnnotationToImport.class)
                .hasNoSuperclass()
                .hasInterfacesMatchingInAnyOrder(Annotation.class)
                .isInterface(true)
                .isEnum(false)
                .isAnnotation(true)
                .isRecord(false);
        assertThat(getAnnotationDefaultValue(javaClass, "someStringMethod", String.class)).isEqualTo("DEFAULT");
        assertThatType(getAnnotationDefaultValue(javaClass, "someTypeMethod", JavaClass.class)).matches(List.class);
        assertThat(getAnnotationDefaultValue(javaClass, "someEnumMethod", JavaEnumConstant.class)).isEquivalentTo(EnumToImport.SECOND);
        assertThatType(getAnnotationDefaultValue(javaClass, "someAnnotationMethod", JavaAnnotation.class).getRawType()).matches(AnnotationParameter.class);
    }

    @Test
    public void imports_annotation_defaults() {
        JavaClass annotationType = new ClassFileImporter().importPackagesOf(TypeAnnotationWithEnumAndArrayValue.class).get(TypeAnnotationWithEnumAndArrayValue.class);

        assertThat((JavaEnumConstant) annotationType.getMethod("valueWithDefault")
                .getDefaultValue().get())
                .as("default of valueWithDefault()").isEquivalentTo(SOME_VALUE);
        assertThat(((JavaEnumConstant[]) annotationType.getMethod("enumArrayWithDefault")
                .getDefaultValue().get()))
                .as("default of enumArrayWithDefault()").matches(OTHER_VALUE);
        assertThat(((JavaAnnotation<?>) annotationType.getMethod("subAnnotationWithDefault")
                .getDefaultValue().get()).get("value").get())
                .as("default of subAnnotationWithDefault()").isEqualTo("default");
        assertThat(((JavaAnnotation<?>[]) annotationType.getMethod("subAnnotationArrayWithDefault")
                .getDefaultValue().get())[0].get("value").get())
                .as("default of subAnnotationArrayWithDefault()").isEqualTo("first");
        assertThatType((JavaClass) annotationType.getMethod("clazzWithDefault")
                .getDefaultValue().get())
                .as("default of clazzWithDefault()").matches(String.class);
        assertThat((JavaClass[]) annotationType.getMethod("classesWithDefault")
                .getDefaultValue().get())
                .as("default of clazzWithDefault()").matchExactly(Serializable.class, List.class);
    }

    @Test
    public void distinguishes_between_explicitly_set_values_and_default_values() {
        @SuppressWarnings("DefaultAnnotationParam")
        @SimpleAnnotationWithDefaultValues(
                // this value is explicitly set to a different value than the default
                defaultValue2 = "overwritten",
                // this value is explicitly set, but actually the same as the default
                defaultValue3 = "defaultValue3",
                noDefault = "mustBeSet")
        class AnnotatedClass {
        }

        JavaAnnotation<?> annotation = new ClassFileImporter().importClasses(AnnotatedClass.class)
                .get(AnnotatedClass.class).getAnnotationOfType(SimpleAnnotationWithDefaultValues.class.getName());

        assertThatAnnotation(annotation)
                .hasExplicitlyDeclaredStringProperty("defaultValue2", "overwritten")
                .hasExplicitlyDeclaredStringProperty("defaultValue3", "defaultValue3")
                .hasExplicitlyDeclaredStringProperty("noDefault", "mustBeSet")

                .hasNoExplicitlyDeclaredProperty("defaultValue1")
                .hasStringProperty("defaultValue1", "defaultValue1");
    }

    @Test
    public void imports_class_with_one_annotation_correctly() {
        JavaClass clazz = new ClassFileImporter().importPackagesOf(ClassWithOneAnnotation.class)
                .get(ClassWithOneAnnotation.class);

        JavaAnnotation<JavaClass> annotation = clazz.getAnnotationOfType(SimpleAnnotation.class.getName());
        assertThatType(annotation.getRawType()).matches(SimpleAnnotation.class);
        assertThatType(annotation.getOwner()).isEqualTo(clazz);

        JavaAnnotation<?> annotationByName = clazz.getAnnotationOfType(SimpleAnnotation.class.getName());
        assertThat(annotationByName).isEqualTo(annotation);

        assertThat(annotation.get("value").get()).isEqualTo("test");

        assertThatType(clazz).matches(ClassWithOneAnnotation.class);
    }

    @Test
    public void class_handles_optional_annotation_correctly() {
        JavaClass clazz = new ClassFileImporter().importPackagesOf(ClassWithOneAnnotation.class)
                .get(ClassWithOneAnnotation.class);

        assertThat(clazz.tryGetAnnotationOfType(SimpleAnnotation.class)).isPresent();
        assertThat(clazz.tryGetAnnotationOfType(Deprecated.class)).isAbsent();
    }

    @Test
    public void imports_class_with_complex_annotations_correctly() {
        JavaClass clazz = new ClassFileImporter().importPackagesOf(ClassWithComplexAnnotations.class)
                .get(ClassWithComplexAnnotations.class);

        assertThat(clazz.getAnnotations()).as("annotations of " + clazz.getSimpleName()).hasSize(2);

        JavaAnnotation<JavaClass> annotation = clazz.getAnnotationOfType(TypeAnnotationWithEnumAndArrayValue.class.getName());
        assertThat((JavaEnumConstant) annotation.get("value").get()).isEquivalentTo(OTHER_VALUE);
        assertThat((JavaEnumConstant) annotation.get("valueWithDefault").get()).isEquivalentTo(SOME_VALUE);
        assertThat(((JavaEnumConstant[]) annotation.get("enumArray").get())).matches(SOME_VALUE, OTHER_VALUE);
        assertThat(((JavaEnumConstant[]) annotation.get("enumArrayWithDefault").get())).matches(OTHER_VALUE);
        JavaAnnotation<?> subAnnotation = (JavaAnnotation<?>) annotation.get("subAnnotation").get();
        assertThat(subAnnotation.get("value").get()).isEqualTo("sub");
        assertThat(subAnnotation.getOwner()).isEqualTo(annotation);
        assertThat(subAnnotation.getAnnotatedElement()).isEqualTo(clazz);
        assertThat(((JavaAnnotation<?>) annotation.get("subAnnotationWithDefault").get()).get("value").get())
                .isEqualTo("default");
        JavaAnnotation<?>[] subAnnotationArray = (JavaAnnotation<?>[]) annotation.get("subAnnotationArray").get();
        assertThat(subAnnotationArray[0].get("value").get()).isEqualTo("otherFirst");
        assertThat(subAnnotationArray[0].getOwner()).isEqualTo(annotation);
        assertThat(subAnnotationArray[0].getAnnotatedElement()).isEqualTo(clazz);
        assertThat(((JavaAnnotation<?>[]) annotation.get("subAnnotationArrayWithDefault").get())[0].get("value").get())
                .isEqualTo("first");
        assertThatType((JavaClass) annotation.get("clazz").get()).matches(Serializable.class);
        assertThatType((JavaClass) annotation.get("clazzWithDefault").get()).matches(String.class);
        assertThat((JavaClass[]) annotation.get("classes").get()).matchExactly(Serializable.class, String.class);
        assertThat((JavaClass[]) annotation.get("classesWithDefault").get()).matchExactly(Serializable.class, List.class);

        assertThatType(clazz).matches(ClassWithComplexAnnotations.class);
    }

    @Test
    public void imports_class_with_annotation_with_empty_array() {
        JavaClass clazz = new ClassFileImporter().importPackagesOf(ClassWithAnnotationWithEmptyArrays.class)
                .get(ClassWithAnnotationWithEmptyArrays.class);

        JavaAnnotation<?> annotation = clazz.getAnnotationOfType(ClassAnnotationWithArrays.class.getName());

        assertThat(Array.getLength(annotation.get("primitives").get())).isZero();
        assertThat(Array.getLength(annotation.get("objects").get())).isZero();
        assertThat(Array.getLength(annotation.get("enums").get())).isZero();
        assertThat(Array.getLength(annotation.get("classes").get())).isZero();
        assertThat(Array.getLength(annotation.get("annotations").get())).isZero();

        ClassAnnotationWithArrays reflected = clazz.getAnnotationOfType(ClassAnnotationWithArrays.class);
        assertThat(reflected.primitives()).isEmpty();
        assertThat(reflected.objects()).isEmpty();
        assertThat(reflected.enums()).isEmpty();
        assertThat(reflected.classes()).isEmpty();
        assertThat(reflected.annotations()).isEmpty();
    }

    @Test
    public void imports_fields_with_one_annotation_correctly() throws Exception {
        JavaField field = new ClassFileImporter().importPackagesOf(ClassWithAnnotatedFields.class)
                .get(ClassWithAnnotatedFields.class).getField("stringAnnotatedField");

        JavaAnnotation<JavaField> annotation = field.getAnnotationOfType(ClassWithAnnotatedFields.FieldAnnotationWithStringValue.class.getName());
        assertThatType(annotation.getRawType()).matches(ClassWithAnnotatedFields.FieldAnnotationWithStringValue.class);
        assertThat(annotation.get("value").get()).isEqualTo("something");
        assertThat(annotation.getOwner()).as("owning field").isEqualTo(field);

        assertThat(field).isEquivalentTo(field.getOwner().reflect().getDeclaredField("stringAnnotatedField"));
    }

    @Test
    public void fields_handle_optional_annotation_correctly() {
        JavaField field = new ClassFileImporter().importPackagesOf(ClassWithAnnotatedFields.class)
                .get(ClassWithAnnotatedFields.class).getField("stringAnnotatedField");

        assertThat(field.tryGetAnnotationOfType(ClassWithAnnotatedFields.FieldAnnotationWithStringValue.class)).isPresent();
        assertThat(field.tryGetAnnotationOfType(ClassWithAnnotatedFields.FieldAnnotationWithEnumClassAndArrayValue.class)).isAbsent();

        Optional<JavaAnnotation<JavaField>> optionalAnnotation = field.tryGetAnnotationOfType(ClassWithAnnotatedFields.FieldAnnotationWithStringValue.class.getName());
        assertThat(optionalAnnotation.get().getOwner()).as("owner of optional annotation").isEqualTo(field);
        assertThat(field.tryGetAnnotationOfType(ClassWithAnnotatedFields.FieldAnnotationWithEnumClassAndArrayValue.class.getName()))
                .as("optional annotation").isAbsent();
    }

    @Test
    public void imports_fields_with_two_annotations_correctly() throws Exception {
        JavaField field = new ClassFileImporter().importPackagesOf(ClassWithAnnotatedFields.class)
                .get(ClassWithAnnotatedFields.class).getField("stringAndIntAnnotatedField");

        Set<JavaAnnotation<JavaField>> annotations = field.getAnnotations();
        assertThat(annotations).hasSize(2);
        assertThat(annotations).extractingResultOf("getOwner").containsOnly(field);
        assertThat(annotations).extractingResultOf("getAnnotatedElement").containsOnly(field);

        JavaAnnotation<JavaField> annotationWithString = field.getAnnotationOfType(ClassWithAnnotatedFields.FieldAnnotationWithStringValue.class.getName());
        assertThat(annotationWithString.get("value").get()).isEqualTo("otherThing");

        JavaAnnotation<JavaField> annotationWithInt = field.getAnnotationOfType(ClassWithAnnotatedFields.FieldAnnotationWithIntValue.class.getName());
        assertThat(annotationWithInt.get("intValue").get()).as("Annotation value with default").isEqualTo(0);
        assertThat(annotationWithInt.get("otherValue").get()).isEqualTo("overridden");

        assertThat(field).isEquivalentTo(field.getOwner().reflect().getDeclaredField("stringAndIntAnnotatedField"));
    }

    @Test
    public void imports_fields_with_complex_annotations_correctly() throws Exception {
        JavaField field = new ClassFileImporter().importPackagesOf(ClassWithAnnotatedFields.class)
                .get(ClassWithAnnotatedFields.class).getField("enumAndArrayAnnotatedField");

        JavaAnnotation<JavaField> annotation = field.getAnnotationOfType(ClassWithAnnotatedFields.FieldAnnotationWithEnumClassAndArrayValue.class.getName());
        assertThat((JavaEnumConstant) annotation.get("value").get()).isEquivalentTo(OTHER_VALUE);
        assertThat((JavaEnumConstant) annotation.get("valueWithDefault").get()).isEquivalentTo(SOME_VALUE);
        assertThat(((JavaEnumConstant[]) annotation.get("enumArray").get())).matches(SOME_VALUE, OTHER_VALUE);
        assertThat(((JavaEnumConstant[]) annotation.get("enumArrayWithDefault").get())).matches(OTHER_VALUE);
        JavaAnnotation<?> subAnnotation = (JavaAnnotation<?>) annotation.get("subAnnotation").get();
        assertThat(subAnnotation.get("value").get()).isEqualTo("changed");
        assertThat(subAnnotation.getOwner()).isEqualTo(annotation);
        assertThat(subAnnotation.getAnnotatedElement()).isEqualTo(field);
        assertThat(((JavaAnnotation<?>) annotation.get("subAnnotationWithDefault").get()).get("value").get())
                .isEqualTo("default");
        JavaAnnotation<?>[] subAnnotationArray = (JavaAnnotation<?>[]) annotation.get("subAnnotationArray").get();
        assertThat(subAnnotationArray[0].get("value").get()).isEqualTo("another");
        assertThat(subAnnotationArray[0].getOwner()).isEqualTo(annotation);
        assertThat(subAnnotation.getAnnotatedElement()).isEqualTo(field);
        assertThat(((JavaAnnotation<?>[]) annotation.get("subAnnotationArrayWithDefault").get())[0].get("value").get())
                .isEqualTo("first");
        assertThatType((JavaClass) annotation.get("clazz").get()).matches(Map.class);
        assertThatType((JavaClass) annotation.get("clazzWithDefault").get()).matches(String.class);
        assertThat((JavaClass[]) annotation.get("classes").get()).matchExactly(Object.class, Serializable.class);
        assertThat((JavaClass[]) annotation.get("classesWithDefault").get()).matchExactly(Serializable.class, List.class);

        assertThat(field).isEquivalentTo(field.getOwner().reflect().getDeclaredField("enumAndArrayAnnotatedField"));
    }

    @Test
    public void imports_fields_with_annotation_with_empty_array() {
        JavaClass clazz = new ClassFileImporter().importPackagesOf(ClassWithAnnotatedFields.class).get(ClassWithAnnotatedFields.class);

        JavaAnnotation<?> annotation = clazz.getField("fieldAnnotatedWithEmptyArrays")
                .getAnnotationOfType(FieldAnnotationWithArrays.class.getName());

        assertThat(Array.getLength(annotation.get("primitives").get())).isZero();
        assertThat(Array.getLength(annotation.get("objects").get())).isZero();
        assertThat(Array.getLength(annotation.get("enums").get())).isZero();
        assertThat(Array.getLength(annotation.get("classes").get())).isZero();
        assertThat(Array.getLength(annotation.get("annotations").get())).isZero();

        FieldAnnotationWithArrays reflected = annotation.as(FieldAnnotationWithArrays.class);
        assertThat(reflected.primitives()).isEmpty();
        assertThat(reflected.objects()).isEmpty();
        assertThat(reflected.enums()).isEmpty();
        assertThat(reflected.classes()).isEmpty();
        assertThat(reflected.annotations()).isEmpty();
    }

    @Test
    public void imports_methods_with_one_annotation_correctly() throws Exception {
        JavaMethod method = new ClassFileImporter().importPackagesOf(ClassWithAnnotatedMethods.class)
                .get(ClassWithAnnotatedMethods.class).getMethod(stringAnnotatedMethod);

        JavaAnnotation<JavaMethod> annotation = method.getAnnotationOfType(ClassWithAnnotatedMethods.MethodAnnotationWithStringValue.class.getName());
        assertThatType(annotation.getRawType()).matches(ClassWithAnnotatedMethods.MethodAnnotationWithStringValue.class);
        assertThat(annotation.getOwner()).isEqualTo(method);
        assertThat(annotation.get("value").get()).isEqualTo("something");

        assertThat(method).isEquivalentTo(ClassWithAnnotatedMethods.class.getMethod(stringAnnotatedMethod));
    }

    @Test
    public void methods_handle_optional_annotation_correctly() {
        JavaMethod method = new ClassFileImporter().importPackagesOf(ClassWithAnnotatedMethods.class)
                .get(ClassWithAnnotatedMethods.class).getMethod(stringAnnotatedMethod);

        assertThat(method.tryGetAnnotationOfType(ClassWithAnnotatedMethods.MethodAnnotationWithStringValue.class)).isPresent();
        assertThat(method.tryGetAnnotationOfType(ClassWithAnnotatedMethods.MethodAnnotationWithEnumAndArrayValue.class)).isAbsent();

        Optional<JavaAnnotation<JavaMethod>> optionalAnnotation = method.tryGetAnnotationOfType(ClassWithAnnotatedMethods.MethodAnnotationWithStringValue.class.getName());
        assertThat(optionalAnnotation.get().getOwner()).as("owner of optional annotation").isEqualTo(method);
        assertThat(method.tryGetAnnotationOfType(ClassWithAnnotatedMethods.MethodAnnotationWithEnumAndArrayValue.class.getName())).isAbsent();
    }

    @Test
    public void imports_methods_with_two_annotations_correctly() throws Exception {
        JavaMethod method = new ClassFileImporter().importPackagesOf(ClassWithAnnotatedMethods.class)
                .get(ClassWithAnnotatedMethods.class).getMethod(stringAndIntAnnotatedMethod);

        Set<JavaAnnotation<JavaMethod>> annotations = method.getAnnotations();
        assertThat(annotations).hasSize(2);
        assertThat(annotations).extractingResultOf("getOwner").containsOnly(method);
        assertThat(annotations).extractingResultOf("getAnnotatedElement").containsOnly(method);

        JavaAnnotation<?> annotationWithString = method.getAnnotationOfType(ClassWithAnnotatedMethods.MethodAnnotationWithStringValue.class.getName());
        assertThat(annotationWithString.get("value").get()).isEqualTo("otherThing");

        JavaAnnotation<?> annotationWithInt = method.getAnnotationOfType(ClassWithAnnotatedMethods.MethodAnnotationWithIntValue.class.getName());
        assertThat(annotationWithInt.get("otherValue").get()).isEqualTo("overridden");

        assertThat(method).isEquivalentTo(ClassWithAnnotatedMethods.class.getMethod(stringAndIntAnnotatedMethod));
    }

    @Test
    public void imports_methods_with_complex_annotations_correctly() throws Exception {
        JavaMethod method = new ClassFileImporter().importPackagesOf(ClassWithAnnotatedMethods.class)
                .get(ClassWithAnnotatedMethods.class).getMethod(enumAndArrayAnnotatedMethod);

        JavaAnnotation<?> annotation = method.getAnnotationOfType(ClassWithAnnotatedMethods.MethodAnnotationWithEnumAndArrayValue.class.getName());
        assertThat((JavaEnumConstant) annotation.get("value").get()).isEquivalentTo(OTHER_VALUE);
        assertThat((JavaEnumConstant) annotation.get("valueWithDefault").get()).isEquivalentTo(SOME_VALUE);
        assertThat(((JavaEnumConstant[]) annotation.get("enumArray").get())).matches(SOME_VALUE, OTHER_VALUE);
        assertThat(((JavaEnumConstant[]) annotation.get("enumArrayWithDefault").get())).matches(OTHER_VALUE);
        JavaAnnotation<?> subAnnotation = (JavaAnnotation<?>) annotation.get("subAnnotation").get();
        assertThat(subAnnotation.get("value").get()).isEqualTo("changed");
        assertThat(subAnnotation.getOwner()).isEqualTo(annotation);
        assertThat(subAnnotation.getAnnotatedElement()).isEqualTo(method);
        assertThat(((JavaAnnotation<?>) annotation.get("subAnnotationWithDefault").get()).get("value").get())
                .isEqualTo("default");
        JavaAnnotation<?>[] subAnnotationArray = (JavaAnnotation<?>[]) annotation.get("subAnnotationArray").get();
        assertThat(subAnnotationArray[0].get("value").get()).isEqualTo("another");
        assertThat(subAnnotationArray[0].getOwner()).isEqualTo(annotation);
        assertThat(subAnnotationArray[0].getAnnotatedElement()).isEqualTo(method);
        assertThat(((JavaAnnotation<?>[]) annotation.get("subAnnotationArrayWithDefault").get())[0].get("value").get())
                .isEqualTo("first");
        assertThatType((JavaClass) annotation.get("clazz").get()).matches(Map.class);
        assertThatType((JavaClass) annotation.get("clazzWithDefault").get()).matches(String.class);
        assertThat((JavaClass[]) annotation.get("classes").get()).matchExactly(Object.class, Serializable.class);
        assertThat((JavaClass[]) annotation.get("classesWithDefault").get()).matchExactly(Serializable.class, List.class);

        assertThat(method).isEquivalentTo(ClassWithAnnotatedMethods.class.getMethod(enumAndArrayAnnotatedMethod));
    }

    @Test
    public void imports_method_with_annotation_with_empty_array() {
        JavaClass clazz = new ClassFileImporter().importPackagesOf(ClassWithAnnotatedMethods.class).get(ClassWithAnnotatedMethods.class);

        JavaAnnotation<?> annotation = clazz.getMethod(methodAnnotatedWithEmptyArrays)
                .getAnnotationOfType(MethodAnnotationWithArrays.class.getName());

        assertThat(Array.getLength(annotation.get("primitives").get())).isZero();
        assertThat(Array.getLength(annotation.get("objects").get())).isZero();
        assertThat(Array.getLength(annotation.get("enums").get())).isZero();
        assertThat(Array.getLength(annotation.get("classes").get())).isZero();
        assertThat(Array.getLength(annotation.get("annotations").get())).isZero();

        MethodAnnotationWithArrays reflected = annotation.as(MethodAnnotationWithArrays.class);
        assertThat(reflected.primitives()).isEmpty();
        assertThat(reflected.objects()).isEmpty();
        assertThat(reflected.enums()).isEmpty();
        assertThat(reflected.classes()).isEmpty();
        assertThat(reflected.annotations()).isEmpty();
    }

    @Test
    public void imports_empty_parameter_annotations_for_method_without_annotated_parameters() {
        JavaMethod method = new ClassFileImporter().importPackagesOf(ClassWithMethodWithAnnotatedParameters.class)
                .get(ClassWithMethodWithAnnotatedParameters.class)
                .getMethod(ClassWithMethodWithAnnotatedParameters.methodWithTwoUnannotatedParameters, String.class, int.class);

        for (JavaParameter parameter : method.getParameters()) {
            assertThat(parameter.getAnnotations()).isEmpty();
        }
        assertThat(method.getParameterAnnotations()).containsExactly(
                Collections.<JavaAnnotation<JavaParameter>>emptySet(), Collections.<JavaAnnotation<JavaParameter>>emptySet());
    }

    @Test
    public void imports_method_with_one_parameter_with_one_annotation() {
        JavaMethod method = new ClassFileImporter().importPackagesOf(ClassWithMethodWithAnnotatedParameters.class)
                .get(ClassWithMethodWithAnnotatedParameters.class)
                .getMethod(ClassWithMethodWithAnnotatedParameters.methodWithOneAnnotatedParameterWithOneAnnotation, String.class);

        List<Set<JavaAnnotation<JavaParameter>>> parameterAnnotations = method.getParameterAnnotations();

        JavaParameter parameter = getOnlyElement(method.getParameters());
        JavaAnnotation<JavaParameter> annotation = getOnlyElement(getOnlyElement(parameterAnnotations));

        assertThat((JavaEnumConstant) annotation.get("value").get()).isEquivalentTo(OTHER_VALUE);
        assertThat((JavaEnumConstant) annotation.get("valueWithDefault").get()).isEquivalentTo(SOME_VALUE);
        assertThat(((JavaEnumConstant[]) annotation.get("enumArray").get())).matches(SOME_VALUE, OTHER_VALUE);
        assertThat(((JavaEnumConstant[]) annotation.get("enumArrayWithDefault").get())).matches(OTHER_VALUE);
        JavaAnnotation<?> subAnnotation = (JavaAnnotation<?>) annotation.get("subAnnotation").get();
        assertThat(subAnnotation.get("value").get()).isEqualTo("changed");
        assertThat(subAnnotation.getOwner()).isEqualTo(annotation);
        assertThat(subAnnotation.getAnnotatedElement()).isEqualTo(parameter);
        assertThat(((JavaAnnotation<?>) annotation.get("subAnnotationWithDefault").get()).get("value").get())
                .isEqualTo("default");
        JavaAnnotation<?>[] subAnnotationArray = (JavaAnnotation<?>[]) annotation.get("subAnnotationArray").get();
        assertThat(subAnnotationArray[0].get("value").get()).isEqualTo("one");
        assertThat(subAnnotationArray[0].getOwner()).isEqualTo(annotation);
        assertThat(subAnnotationArray[0].getAnnotatedElement()).isEqualTo(parameter);
        assertThat(subAnnotationArray[1].get("value").get()).isEqualTo("two");
        assertThat(subAnnotationArray[1].getOwner()).isEqualTo(annotation);
        assertThat(subAnnotationArray[1].getAnnotatedElement()).isEqualTo(parameter);
        assertThat(((JavaAnnotation<?>[]) annotation.get("subAnnotationArrayWithDefault").get())[0].get("value").get())
                .isEqualTo("first");
        assertThatType((JavaClass) annotation.get("clazz").get()).matches(Map.class);
        assertThatType((JavaClass) annotation.get("clazzWithDefault").get()).matches(String.class);
        assertThat((JavaClass[]) annotation.get("classes").get()).matchExactly(Object.class, Serializable.class);
        assertThat((JavaClass[]) annotation.get("classesWithDefault").get()).matchExactly(Serializable.class, List.class);

        assertThatAnnotation(getOnlyElement(parameter.getAnnotations())).matches(method.reflect().getParameterAnnotations()[0][0]);
        assertThatAnnotation(annotation).matches(method.reflect().getParameterAnnotations()[0][0]);
    }

    @Test
    public void imports_method_with_one_parameter_with_two_annotations() {
        JavaMethod method = new ClassFileImporter().importPackagesOf(ClassWithMethodWithAnnotatedParameters.class)
                .get(ClassWithMethodWithAnnotatedParameters.class)
                .getMethod(ClassWithMethodWithAnnotatedParameters.methodWithOneAnnotatedParameterWithTwoAnnotations, String.class);

        List<Set<JavaAnnotation<JavaParameter>>> parameterAnnotations = method.getParameterAnnotations();

        Set<JavaAnnotation<JavaParameter>> annotations = getOnlyElement(parameterAnnotations);

        assertThat(annotations).isEqualTo(getOnlyElement(method.getParameters()).getAnnotations());
        assertThatAnnotations(annotations).match(ImmutableSet.copyOf(method.reflect().getParameterAnnotations()[0]));
    }

    @Test
    public void imports_methods_with_multiple_parameters_with_annotations() {
        JavaMethod method = new ClassFileImporter().importPackagesOf(ClassWithMethodWithAnnotatedParameters.class)
                .get(ClassWithMethodWithAnnotatedParameters.class)
                .getMethod(ClassWithMethodWithAnnotatedParameters.methodWithTwoAnnotatedParameters, String.class, int.class);

        List<Set<JavaAnnotation<JavaParameter>>> parameterAnnotations = method.getParameterAnnotations();

        List<JavaParameter> parameters = method.getParameters();
        for (int i = 0; i < 2; i++) {
            assertThat(parameterAnnotations.get(i)).isEqualTo(parameters.get(i).getAnnotations());
            assertThatAnnotations(parameterAnnotations.get(i)).match(ImmutableSet.copyOf(method.reflect().getParameterAnnotations()[i]));
        }
    }

    @Test
    public void imports_methods_with_multiple_parameters_with_annotations_but_unannotated_parameters_in_between() {
        JavaMethod method = new ClassFileImporter().importPackagesOf(ClassWithMethodWithAnnotatedParameters.class)
                .get(ClassWithMethodWithAnnotatedParameters.class)
                .getMethod(ClassWithMethodWithAnnotatedParameters.methodWithAnnotatedParametersGap, String.class, int.class, Object.class, List.class);

        List<Set<JavaAnnotation<JavaParameter>>> parameterAnnotations = method.getParameterAnnotations();

        assertThatAnnotations(parameterAnnotations.get(0)).isNotEmpty();
        assertThatAnnotations(parameterAnnotations.get(1)).isEmpty();
        assertThatAnnotations(parameterAnnotations.get(2)).isEmpty();
        assertThatAnnotations(parameterAnnotations.get(3)).isNotEmpty();

        List<JavaParameter> parameters = method.getParameters();
        for (int i = 0; i < 4; i++) {
            assertThat(parameterAnnotations.get(i)).isEqualTo(parameters.get(i).getAnnotations());
            assertThatAnnotations(parameterAnnotations.get(i)).match(ImmutableSet.copyOf(method.reflect().getParameterAnnotations()[i]));
        }
    }

    @Test
    public void imports_parameter_annotations_correctly_for_synthetic_constructor_parameter() {
        @SuppressWarnings("unused")
        class LocalClassThusSyntheticFirstConstructorParameter {
            LocalClassThusSyntheticFirstConstructorParameter(Object notAnnotated, @SimpleAnnotation("test") List<String> annotated) {
            }
        }

        JavaConstructor constructor = getOnlyElement(new ClassFileImporter().importClasses(LocalClassThusSyntheticFirstConstructorParameter.class, String.class)
                .get(LocalClassThusSyntheticFirstConstructorParameter.class)
                .getConstructors());

        List<Set<JavaAnnotation<JavaParameter>>> parameterAnnotations = constructor.getParameterAnnotations();

        assertThat(parameterAnnotations).as("parameter annotations").hasSize(2);
        assertThatAnnotations(parameterAnnotations.get(0)).isEmpty();
        assertThatAnnotations(parameterAnnotations.get(1)).isNotEmpty();

        List<JavaParameter> parameters = constructor.getParameters();
        for (int i = 0; i < parameterAnnotations.size(); i++) {
            assertThat(parameterAnnotations.get(i)).isEqualTo(parameters.get(i).getAnnotations());
            assertThatAnnotations(parameterAnnotations.get(i)).match(ImmutableSet.copyOf(constructor.reflect().getParameterAnnotations()[i]));
        }
    }

    @Test
    public void imports_constructors_with_complex_annotations_correctly() throws Exception {
        JavaConstructor constructor = new ClassFileImporter().importPackagesOf(ClassWithAnnotatedMethods.class)
                .get(ClassWithAnnotatedMethods.class).getConstructor();

        JavaAnnotation<JavaConstructor> annotation = constructor.getAnnotationOfType(ClassWithAnnotatedMethods.MethodAnnotationWithEnumAndArrayValue.class.getName());
        assertThat((Object[]) annotation.get("classes").get()).extracting("name")
                .containsExactly(Object.class.getName(), Serializable.class.getName());
        assertThat(annotation.getOwner()).isEqualTo(constructor);
        JavaAnnotation<?> subAnnotation = (JavaAnnotation<?>) annotation.get("subAnnotation").get();
        assertThat(subAnnotation.get("value").get()).isEqualTo("changed");
        assertThat(subAnnotation.getOwner()).isEqualTo(annotation);
        assertThat(subAnnotation.getAnnotatedElement()).isEqualTo(constructor);
        JavaAnnotation<?>[] subAnnotationArray = (JavaAnnotation<?>[]) annotation.get("subAnnotationArray").get();
        assertThat(subAnnotationArray[0].get("value").get()).isEqualTo("another");
        assertThat(subAnnotationArray[0].getOwner()).isEqualTo(annotation);
        assertThat(subAnnotationArray[0].getAnnotatedElement()).isEqualTo(constructor);

        assertThat(constructor).isEquivalentTo(ClassWithAnnotatedMethods.class.getConstructor());
    }

    @Test
    public void classes_know_which_annotations_have_their_type() {
        JavaClasses classes = new ClassFileImporter().importClasses(ClassWithOneAnnotation.class, SimpleAnnotation.class);

        Set<JavaAnnotation<?>> annotations = classes.get(SimpleAnnotation.class).getAnnotationsWithTypeOfSelf();

        assertThat(getOnlyElement(annotations).getOwner()).isEqualTo(classes.get(ClassWithOneAnnotation.class));
    }

    @Test
    public void classes_know_which_annotation_members_have_their_type() {
        @SuppressWarnings("unused")
        @ParameterAnnotation(value = String.class)
        class Dependent {
            @ParameterAnnotation(value = String.class)
            String field;

            @ParameterAnnotation(value = String.class)
            Dependent() {
            }

            @ParameterAnnotation(value = String.class)
            void method() {
            }

            @ParameterAnnotation(value = List.class)
            void notToFind() {
            }
        }

        JavaClasses classes = new ClassFileImporter().importClasses(Dependent.class, ParameterAnnotation.class, String.class);
        Set<JavaAnnotation<?>> annotations = classes.get(String.class).getAnnotationsWithParameterTypeOfSelf();

        for (JavaAnnotation<?> annotation : annotations) {
            assertThatAnnotation(annotation).hasType(ParameterAnnotation.class);
        }

        Set<JavaAnnotation<?>> expected = ImmutableSet.<JavaAnnotation<?>>of(
                classes.get(Dependent.class).getAnnotationOfType(ParameterAnnotation.class.getName()),
                classes.get(Dependent.class).getField("field").getAnnotationOfType(ParameterAnnotation.class.getName()),
                classes.get(Dependent.class).getConstructor(getClass()).getAnnotationOfType(ParameterAnnotation.class.getName()),
                classes.get(Dependent.class).getMethod("method").getAnnotationOfType(ParameterAnnotation.class.getName())
        );
        assertThat(annotations).as("annotations with parameter type " + String.class.getSimpleName()).containsOnlyElementsOf(expected);
    }

    @SuppressWarnings({"unchecked", "unused"})
    private static <T> T getAnnotationDefaultValue(JavaClass javaClass, String methodName, Class<T> valueType) {
        return (T) javaClass.getMethod(methodName).getDefaultValue().get();
    }

    @SuppressWarnings("unused")
    private @interface SimpleAnnotationWithDefaultValues {
        String defaultValue1() default "defaultValue1";

        String defaultValue2() default "defaultValue2";

        String defaultValue3() default "defaultValue3";

        String noDefault();
    }

    @SuppressWarnings("unused")
    private @interface MetaAnnotationWithParameters {
        SomeEnum someEnum();

        SomeEnum someEnumDefault() default SomeEnum.VARIABLE;

        ParameterAnnotation parameterAnnotation();

        ParameterAnnotation parameterAnnotationDefault() default @ParameterAnnotation(Integer.class);
    }

    @SuppressWarnings("unused")
    private @interface SomeMetaMetaMetaAnnotationWithParameters {
        Class<?> classParam();

        Class<?> classParamDefault() default String.class;

        SomeMetaMetaMetaAnnotationEnumParameter enumParam();

        SomeMetaMetaMetaAnnotationEnumParameter enumParamDefault() default SomeMetaMetaMetaAnnotationEnumParameter.CONSTANT;

        SomeMetaMetaMetaParameterAnnotation annotationParam();

        SomeMetaMetaMetaParameterAnnotation annotationParamDefault() default @SomeMetaMetaMetaParameterAnnotation(Boolean.class);
    }

    @SomeMetaMetaMetaAnnotationWithParameters(
            classParam = SomeMetaMetaMetaAnnotationClassParameter.class,
            enumParam = SomeMetaMetaMetaAnnotationEnumParameter.VALUE,
            annotationParam = @SomeMetaMetaMetaParameterAnnotation(SomeMetaMetaMetaParameterAnnotationClassParameter.class)
    )
    private @interface SomeMetaMetaAnnotation {
    }

    @SomeMetaMetaAnnotation
    private @interface SomeMetaAnnotation {
    }

    @MetaAnnotationWithParameters(
            someEnum = SomeEnum.CONSTANT,
            parameterAnnotation = @ParameterAnnotation(SomeAnnotationParameterType.class)
    )
    @SomeMetaAnnotation
    private @interface MetaAnnotatedAnnotation {
    }

    private enum SomeEnum {
        CONSTANT,
        VARIABLE
    }

    private @interface ParameterAnnotation {
        Class<?> value();
    }

    private static class SomeAnnotationParameterType {
    }


    @SuppressWarnings("unused")
    private static class ClassWithMetaAnnotatedField {
        @MetaAnnotatedAnnotation
        int metaAnnotatedField;
    }

    @SuppressWarnings("unused")
    private static class ClassWithMetaAnnotatedMethod {
        @MetaAnnotatedAnnotation
        void metaAnnotatedMethod() {
        }
    }

    @SuppressWarnings("unused")
    private static class ClassWithMetaAnnotatedConstructor {
        @MetaAnnotatedAnnotation
        ClassWithMetaAnnotatedConstructor() {
        }
    }

    @SuppressWarnings("unused")
    private static class ClassWithMetaAnnotatedConstructorParameter {
        ClassWithMetaAnnotatedConstructorParameter(@MetaAnnotatedAnnotation String param) {
        }
    }

    @SuppressWarnings("unused")
    private static class ClassWithMetaAnnotatedMethodParameter {
        void method(@MetaAnnotatedAnnotation String param) {
        }
    }

    private static class SomeMetaMetaMetaAnnotationClassParameter {
    }

    private enum SomeMetaMetaMetaAnnotationEnumParameter {
        VALUE,
        CONSTANT
    }

    private @interface SomeMetaMetaMetaParameterAnnotation {
        Class<?> value();
    }

    private static class SomeMetaMetaMetaParameterAnnotationClassParameter {
    }
}
