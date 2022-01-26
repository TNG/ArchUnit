package com.tngtech.archunit.core.importer;

import java.io.File;
import java.io.PrintStream;
import java.io.Serializable;
import java.nio.Buffer;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.function.Supplier;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.domain.InstanceofCheck;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaConstructorReference;
import com.tngtech.archunit.core.domain.JavaEnumConstant;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaMethodReference;
import com.tngtech.archunit.core.domain.ReferencedClassObject;
import com.tngtech.archunit.core.domain.ThrowsDeclaration;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.archunit.core.importer.testexamples.SomeAnnotation;
import com.tngtech.archunit.core.importer.testexamples.annotatedclassimport.ClassWithUnimportedAnnotation;
import com.tngtech.archunit.core.importer.testexamples.annotatedparameters.ClassWithMethodWithAnnotatedParameters;
import com.tngtech.archunit.core.importer.testexamples.annotatedparameters.ClassWithMethodWithAnnotatedParameters.SomeParameterAnnotation;
import com.tngtech.archunit.core.importer.testexamples.annotationfieldimport.ClassWithAnnotatedFields;
import com.tngtech.archunit.core.importer.testexamples.annotationmethodimport.ClassWithAnnotatedMethods;
import com.tngtech.archunit.core.importer.testexamples.annotationmethodimport.ClassWithAnnotatedMethods.MethodAnnotationWithEnumAndArrayValue;
import com.tngtech.archunit.core.importer.testexamples.annotations.AnotherAnnotationWithAnnotationParameter;
import com.tngtech.archunit.core.importer.testexamples.annotations.SomeAnnotationWithAnnotationParameter;
import com.tngtech.archunit.core.importer.testexamples.annotations.SomeAnnotationWithClassParameter;
import com.tngtech.archunit.core.importer.testexamples.classhierarchy.Child;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.importer.testexamples.SomeEnum.OTHER_VALUE;
import static com.tngtech.archunit.core.importer.testexamples.SomeEnum.SOME_VALUE;
import static com.tngtech.archunit.core.importer.testexamples.annotatedparameters.ClassWithMethodWithAnnotatedParameters.methodWithOneAnnotatedParameterWithTwoAnnotations;
import static com.tngtech.archunit.core.importer.testexamples.annotationmethodimport.ClassWithAnnotatedMethods.methodAnnotatedWithAnnotationFromParentPackage;
import static com.tngtech.archunit.testutil.ArchConfigurationRule.resetConfigurationAround;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static com.tngtech.archunit.testutil.Assertions.assertThatAnnotation;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.assertion.JavaAnnotationAssertion.annotationProperty;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;

@RunWith(DataProviderRunner.class)
public class ClassFileImporterAutomaticResolutionTest {

    @Test
    public void automatically_resolves_field_types() {
        @SuppressWarnings("unused")
        class FieldTypeWithoutAnyFurtherReference {
            String field;
        }

        JavaClass javaClass = new ClassFileImporter().importClass(FieldTypeWithoutAnyFurtherReference.class);

        assertThat(javaClass.getField("field").getRawType()).as("field type").isFullyImported(true);
    }

    @Test
    public void automatically_resolves_constructor_parameter_types() {
        @SuppressWarnings("unused")
        class ConstructorParameterTypesWithoutAnyFurtherReference {
            ConstructorParameterTypesWithoutAnyFurtherReference(FileSystem constructorParam1, Buffer constructorParam2) {
            }
        }

        JavaClass javaClass = new ClassFileImporter().importClass(ConstructorParameterTypesWithoutAnyFurtherReference.class);

        JavaConstructor constructor = javaClass.getConstructor(getClass(), FileSystem.class, Buffer.class);
        assertThat(constructor.getRawParameterTypes().get(0)).as("constructor parameter type").isFullyImported(true);
        assertThat(constructor.getRawParameterTypes().get(1)).as("constructor parameter type").isFullyImported(true);
    }

    @Test
    public void automatically_resolves_method_return_types() {
        @SuppressWarnings("unused")
        class MemberTypesWithoutAnyFurtherReference {
            File returnType() {
                return null;
            }
        }

        JavaClass javaClass = new ClassFileImporter().importClass(MemberTypesWithoutAnyFurtherReference.class);

        assertThat(javaClass.getMethod("returnType").getRawReturnType()).as("method return type").isFullyImported(true);
    }

    @Test
    public void automatically_resolves_method_parameter_types() {
        @SuppressWarnings("unused")
        class MemberTypesWithoutAnyFurtherReference {
            void methodParameters(Path methodParam1, PrintStream methodParam2) {
            }
        }

        JavaClass javaClass = new ClassFileImporter().importClass(MemberTypesWithoutAnyFurtherReference.class);

        JavaMethod method = javaClass.getMethod("methodParameters", Path.class, PrintStream.class);
        assertThat(method.getRawParameterTypes().get(0)).as("method parameter type").isFullyImported(true);
        assertThat(method.getRawParameterTypes().get(1)).as("method parameter type").isFullyImported(true);
    }

    @Test
    public void automatically_resolves_class_hierarchy() {
        JavaClass child = new ClassFileImporter().importClass(Child.class);

        JavaClass parent = child.getRawSuperclass().get();
        assertThat(parent).isFullyImported(true);

        JavaClass grandparent = parent.getRawSuperclass().get();
        assertThat(grandparent).isFullyImported(true);

        JavaClass parentInterfaceDirect = getOnlyElement(child.getRawInterfaces());
        assertThat(parentInterfaceDirect).isFullyImported(true);

        JavaClass grandParentInterfaceDirect = getOnlyElement(parentInterfaceDirect.getRawInterfaces());
        assertThat(grandParentInterfaceDirect).isFullyImported(true);

        JavaClass grandParentInterfaceIndirect = getOnlyElement(getOnlyElement(grandparent.getRawInterfaces()).getRawInterfaces());
        assertThat(grandParentInterfaceIndirect).isFullyImported(true);
    }

    @Test
    public void automatically_resolves_class_annotations() {
        JavaClass clazz = new ClassFileImporter().importClass(ClassWithUnimportedAnnotation.class);

        JavaAnnotation<?> annotation = clazz.getAnnotationOfType(SomeAnnotation.class.getName());

        assertThat(annotation.getRawType()).isFullyImported(true);

        assertThat(annotation.get("mandatory")).contains("mandatory");
        assertThat(annotation.get("optional")).contains("optional");
        assertThat((JavaEnumConstant) annotation.get("mandatoryEnum").get()).isEquivalentTo(SOME_VALUE);
        assertThat((JavaEnumConstant) annotation.get("optionalEnum").get()).isEquivalentTo(OTHER_VALUE);

        SomeAnnotation reflected = clazz.getAnnotationOfType(SomeAnnotation.class);
        assertThat(reflected.mandatory()).isEqualTo("mandatory");
        assertThat(reflected.optional()).isEqualTo("optional");
        assertThat(reflected.mandatoryEnum()).isEqualTo(SOME_VALUE);
        assertThat(reflected.optionalEnum()).isEqualTo(OTHER_VALUE);
    }

    @Test
    public void automatically_resolves_field_annotations() {
        JavaClass clazz = new ClassFileImporter().importClass(ClassWithAnnotatedFields.class);

        JavaAnnotation<?> annotation = clazz.getField("fieldAnnotatedWithAnnotationFromParentPackage")
                .getAnnotationOfType(SomeAnnotation.class.getName());

        assertThat(annotation.getRawType()).isFullyImported(true);

        assertThat(annotation.get("mandatory")).contains("mandatory");
        assertThat(annotation.get("optional")).contains("optional");

        SomeAnnotation reflected = annotation.as(SomeAnnotation.class);
        assertThat(reflected.mandatory()).isEqualTo("mandatory");
        assertThat(reflected.optional()).isEqualTo("optional");
    }

    @Test
    public void automatically_resolves_method_annotations() {
        JavaClass clazz = new ClassFileImporter().importClass(ClassWithAnnotatedMethods.class);

        JavaAnnotation<?> annotation = clazz.getMethod(methodAnnotatedWithAnnotationFromParentPackage)
                .getAnnotationOfType(SomeAnnotation.class.getName());

        assertThat(annotation.getRawType()).isFullyImported(true);

        assertThat(annotation.get("mandatory")).contains("mandatory");
        assertThat(annotation.get("optional")).contains("optional");

        SomeAnnotation reflected = annotation.as(SomeAnnotation.class);
        assertThat(reflected.mandatory()).isEqualTo("mandatory");
        assertThat(reflected.optional()).isEqualTo("optional");
    }

    @Test
    public void automatically_resolves_constructor_annotations() {
        JavaClass clazz = new ClassFileImporter().importClass(ClassWithAnnotatedMethods.class);

        JavaAnnotation<?> annotation = clazz.getConstructor()
                .getAnnotationOfType(MethodAnnotationWithEnumAndArrayValue.class.getName());

        assertThat(annotation.getRawType()).isFullyImported(true);

        assertThat((JavaEnumConstant) annotation.get("value").get()).isEquivalentTo(OTHER_VALUE);

        MethodAnnotationWithEnumAndArrayValue reflected = annotation.as(MethodAnnotationWithEnumAndArrayValue.class);
        assertThat(reflected.value()).isEqualTo(OTHER_VALUE);
    }

    @Test
    public void automatically_resolves_parameter_annotations() {
        JavaClass clazz = new ClassFileImporter().importClass(ClassWithMethodWithAnnotatedParameters.class);

        JavaAnnotation<?> annotation = clazz.getMethod(methodWithOneAnnotatedParameterWithTwoAnnotations, String.class)
                .getParameters().get(0)
                .getAnnotationOfType(SomeParameterAnnotation.class.getName());

        assertThat(annotation.getRawType()).isFullyImported(true);

        assertThat((JavaEnumConstant) annotation.get("value").get()).isEquivalentTo(OTHER_VALUE);
        assertThat((JavaEnumConstant) annotation.get("valueWithDefault").get()).isEquivalentTo(SOME_VALUE);

        SomeParameterAnnotation reflected = annotation.as(SomeParameterAnnotation.class);
        assertThat(reflected.value()).isEqualTo(OTHER_VALUE);
        assertThat(reflected.valueWithDefault()).isEqualTo(SOME_VALUE);
    }

    @Test
    public void automatically_resolves_meta_annotation_types() {
        JavaClass javaClass = new ClassFileImporter().importClass(MetaAnnotatedClass.class);
        JavaAnnotation<JavaClass> someAnnotation = javaClass.getAnnotationOfType(MetaAnnotatedAnnotation.class.getName());
        JavaAnnotation<JavaClass> someMetaAnnotation = someAnnotation.getRawType()
                .getAnnotationOfType(SomeMetaAnnotation.class.getName());
        JavaAnnotation<JavaClass> someMetaMetaAnnotation = someMetaAnnotation.getRawType()
                .getAnnotationOfType(SomeMetaMetaAnnotation.class.getName());
        JavaAnnotation<JavaClass> someMetaMetaMetaAnnotation = someMetaMetaAnnotation.getRawType()
                .getAnnotationOfType(SomeMetaMetaMetaAnnotationWithParameters.class.getName());

        assertThatType(someMetaMetaMetaAnnotation.getType()).matches(SomeMetaMetaMetaAnnotationWithParameters.class);
    }

    @DataProvider
    public static Object[][] elementsAnnotatedWithSomeAnnotation() {
        return testForEach(
                new ClassFileImporter().importClass(MetaAnnotatedClass.class),
                new ClassFileImporter().importClass(ClassWithMetaAnnotatedField.class).getField("metaAnnotatedField"),
                new ClassFileImporter().importClass(ClassWithMetaAnnotatedMethod.class).getMethod("metaAnnotatedMethod"),
                new ClassFileImporter().importClass(ClassWithMetaAnnotatedConstructor.class).getConstructor(),
                getOnlyElement(new ClassFileImporter().importClass(ClassWithMetaAnnotatedConstructorParameter.class).getConstructor(String.class).getParameters()),
                getOnlyElement(new ClassFileImporter().importClass(ClassWithMetaAnnotatedMethodParameter.class).getMethod("method", String.class).getParameters())
        );
    }

    @Test
    @UseDataProvider("elementsAnnotatedWithSomeAnnotation")
    public void automatically_resolves_parameters_of_meta_annotations(HasAnnotations<?> annotatedWithSomeAnnotation) {
        JavaAnnotation<?> someAnnotation = annotatedWithSomeAnnotation
                .getAnnotationOfType(MetaAnnotatedAnnotation.class.getName());
        JavaAnnotation<?> metaAnnotationWithParameters = someAnnotation.getRawType()
                .getAnnotationOfType(MetaAnnotationWithParameters.class.getName());

        assertThatAnnotation(metaAnnotationWithParameters)
                .hasEnumProperty("someEnum", SomeAnnotationEnum.CONSTANT)
                .hasEnumProperty("someEnumDefault", SomeAnnotationEnum.VARIABLE)
                .hasAnnotationProperty("parameterAnnotation",
                        annotationProperty()
                                .withAnnotationType(ParameterAnnotation.class)
                                .withClassProperty("value", SomeAnnotationParameterType.class))
                .hasAnnotationProperty("parameterAnnotationDefault",
                        annotationProperty()
                                .withAnnotationType(ParameterAnnotation.class)
                                .withClassProperty("value", SomeAnnotationDefaultParameterType.class));

        JavaAnnotation<JavaClass> metaMetaMetaAnnotation = someAnnotation
                .getRawType().getAnnotationOfType(SomeMetaAnnotation.class.getName())
                .getRawType().getAnnotationOfType(SomeMetaMetaAnnotation.class.getName())
                .getRawType().getAnnotationOfType(SomeMetaMetaMetaAnnotationWithParameters.class.getName());

        assertThatAnnotation(metaMetaMetaAnnotation)
                .hasClassProperty("classParam", SomeMetaMetaMetaAnnotationClassParameter.class)
                .hasClassProperty("classParamDefault", String.class)
                .hasEnumProperty("enumParam", SomeMetaMetaMetaAnnotationEnumParameter.VALUE)
                .hasEnumProperty("enumParamDefault", SomeMetaMetaMetaAnnotationEnumParameter.CONSTANT)
                .hasAnnotationProperty("annotationParam",
                        annotationProperty()
                                .withAnnotationType(SomeMetaMetaMetaParameterAnnotation.class)
                                .withClassProperty("value", SomeMetaMetaMetaParameterAnnotationClassParameter.class))
                .hasAnnotationProperty("annotationParamDefault",
                        annotationProperty()
                                .withAnnotationType(SomeMetaMetaMetaParameterAnnotation.class));
    }

    @Test
    public void automatically_resolves_field_access_target_owners() {
        class Target {
            String field;
        }
        @SuppressWarnings({"unused", "ConstantConditions"})
        class Origin {
            Object resolvesFieldAccessOwner() {
                Target target = null;
                return target.field;
            }
        }

        JavaFieldAccess access = getOnlyElement(new ClassFileImporter().importClass(Origin.class).getMethod("resolvesFieldAccessOwner").getFieldAccesses());

        assertThat(access.getTargetOwner()).isFullyImported(true);
    }

    @Test
    public void automatically_resolves_method_call_target_owners() {
        class Target {
            void method() {
            }
        }
        @SuppressWarnings({"unused", "ConstantConditions"})
        class Origin {
            void resolvesMethodCallTargetOwner() {
                Target target = null;
                target.method();
            }
        }

        JavaMethodCall call = getOnlyElement(new ClassFileImporter().importClass(Origin.class).getMethodCallsFromSelf());

        assertThat(call.getTargetOwner()).isFullyImported(true);
    }

    @Test
    public void automatically_resolves_method_reference_target_owners() {
        class Target {
            void method() {
            }
        }
        @SuppressWarnings({"unused", "ConstantConditions"})
        class Origin {
            Runnable resolvesMethodCallTargetOwner() {
                Target target = null;
                return target::method;
            }
        }

        JavaMethodReference reference = getOnlyElement(new ClassFileImporter().importClass(Origin.class).getMethodReferencesFromSelf());

        assertThat(reference.getTargetOwner()).isFullyImported(true);
    }

    @Test
    public void automatically_resolves_constructor_call_target_owners() {
        class Target {
        }
        @SuppressWarnings("unused")
        class Origin {
            void resolvesConstructorCallTargetOwner() {
                new Target();
            }
        }

        JavaClass javaClass = new ClassFileImporter().importClass(Origin.class);
        JavaConstructorCall call = getOnlyElement(javaClass.getMethod("resolvesConstructorCallTargetOwner").getConstructorCallsFromSelf());

        assertThat(call.getTargetOwner()).isFullyImported(true);
    }

    private static class Data_automatically_resolves_constructor_reference_target_owners {
        static class Target {
        }
    }

    @Test
    public void automatically_resolves_constructor_reference_target_owners() {
        @SuppressWarnings("unused")
        class Origin {
            Supplier<?> resolvesConstructorReferenceTargetOwner() {
                return Data_automatically_resolves_constructor_reference_target_owners.Target::new;
            }
        }

        JavaClass javaClass = new ClassFileImporter().importClass(Origin.class);
        JavaConstructorReference reference = getOnlyElement(javaClass.getMethod("resolvesConstructorReferenceTargetOwner").getConstructorReferencesFromSelf());

        assertThat(reference.getTargetOwner()).isFullyImported(true);
    }

    @Test
    public void never_reports_stub_classes_as_fully_imported() {
        @SuppressWarnings("unused")
        class SomeClass {
            Serializable withStubType;
        }

        JavaClass javaClass = resetConfigurationAround(() -> {
            ArchConfiguration.get().setResolveMissingDependenciesFromClassPath(false);
            return new ClassFileImporter().importClass(SomeClass.class);
        });

        JavaClass stubType = javaClass.getField("withStubType").getRawType();

        // if we don't resolve the class from the classpath we don't know if it's an interface
        assertThat(stubType).isInterface(false);
        // then we also don't want to claim this class is fully imported, even though it went through the
        // resolution steps to complete the hierarchy, etc.
        assertThat(stubType).isFullyImported(false);
    }

    @DataProvider
    public static Object[][] data_automatically_resolves_annotation_parameter_types() {
        @SomeAnnotationWithClassParameter(String.class)
        @SomeAnnotationWithAnnotationParameter(@SomeAnnotationWithClassParameter(File.class))
        @AnotherAnnotationWithAnnotationParameter(@SomeAnnotationWithAnnotationParameter(@SomeAnnotationWithClassParameter(Serializable.class)))
        class OnClass {
        }
        @SuppressWarnings("unused")
        class OnField {
            @SomeAnnotationWithClassParameter(String.class)
            @SomeAnnotationWithAnnotationParameter(@SomeAnnotationWithClassParameter(File.class))
            @AnotherAnnotationWithAnnotationParameter(@SomeAnnotationWithAnnotationParameter(@SomeAnnotationWithClassParameter(Serializable.class)))
            Object field;
        }
        @SuppressWarnings("unused")
        class OnMethod {
            @SomeAnnotationWithClassParameter(String.class)
            @SomeAnnotationWithAnnotationParameter(@SomeAnnotationWithClassParameter(File.class))
            @AnotherAnnotationWithAnnotationParameter(@SomeAnnotationWithAnnotationParameter(@SomeAnnotationWithClassParameter(Serializable.class)))
            void method() {
            }
        }
        class OnConstructor {
            @SomeAnnotationWithClassParameter(String.class)
            @SomeAnnotationWithAnnotationParameter(@SomeAnnotationWithClassParameter(File.class))
            @AnotherAnnotationWithAnnotationParameter(@SomeAnnotationWithAnnotationParameter(@SomeAnnotationWithClassParameter(Serializable.class)))
            OnConstructor() {
            }
        }
        class OnParameter {
            OnParameter(
                    @SomeAnnotationWithClassParameter(String.class)
                    @SomeAnnotationWithAnnotationParameter(@SomeAnnotationWithClassParameter(File.class))
                    @AnotherAnnotationWithAnnotationParameter(@SomeAnnotationWithAnnotationParameter(@SomeAnnotationWithClassParameter(Serializable.class))) Object parameter) {
            }
        }

        return FluentIterable.concat(
                nestedAnnotationValueTestCases(new ClassFileImporter().importClass(OnClass.class)),
                nestedAnnotationValueTestCases(new ClassFileImporter().importClass(OnField.class).getField("field")),
                nestedAnnotationValueTestCases(new ClassFileImporter().importClass(OnMethod.class).getMethod("method")),
                nestedAnnotationValueTestCases(getOnlyElement(new ClassFileImporter().importClass(OnConstructor.class).getConstructors())),
                nestedAnnotationValueTestCases(getOnlyElement(new ClassFileImporter().importClass(OnParameter.class).getConstructors()).getParameters().get(0))
        ).toArray(Object[].class);
    }

    private static Iterable<Object[]> nestedAnnotationValueTestCases(HasAnnotations<?> hasAnnotations) {
        return ImmutableList.of(
                $(getNestedAnnotationClassValue(hasAnnotations, SomeAnnotationWithClassParameter.class), expect(String.class)),
                $(getNestedAnnotationClassValue(hasAnnotations, SomeAnnotationWithAnnotationParameter.class), expect(File.class)),
                $(getNestedAnnotationClassValue(hasAnnotations, AnotherAnnotationWithAnnotationParameter.class), expect(Serializable.class))
        );
    }

    private static JavaClass getNestedAnnotationClassValue(HasAnnotations<?> hasAnnotations, Class<?> annotationType) {
        Object value = hasAnnotations.getAnnotationOfType(annotationType.getName());
        while (!(value instanceof JavaClass)) {
            value = ((JavaAnnotation<?>) value).get("value").get();
        }
        return (JavaClass) value;
    }

    // just syntactic sugar to improve readability what is actual and what is expected
    private static Class<?> expect(Class<?> clazz) {
        return clazz;
    }

    @Test
    @UseDataProvider
    public void test_automatically_resolves_annotation_parameter_types(JavaClass annotationValue, Class<?> expectedType) {
        assertThat(annotationValue).isFullyImported(true);
        assertThatType(annotationValue).matches(expectedType);
    }

    @Test
    public void automatically_resolves_class_objects() {
        @SuppressWarnings("unused")
        class Origin {
            Class<?> call() {
                return String.class;
            }
        }

        ReferencedClassObject classObject = getOnlyElement(new ClassFileImporter().importClass(Origin.class).getReferencedClassObjects());

        assertThat(classObject.getRawType()).isFullyImported(true);
        assertThatType(classObject.getRawType()).matches(String.class);
    }

    @Test
    public void automatically_resolves_instanceof_check_targets() {
        @SuppressWarnings("unused")
        class Origin {
            boolean call(Object obj) {
                return obj instanceof String;
            }
        }

        InstanceofCheck instanceofCheck = getOnlyElement(new ClassFileImporter().importClass(Origin.class).getInstanceofChecks());

        assertThat(instanceofCheck.getRawType()).isFullyImported(true);
        assertThatType(instanceofCheck.getRawType()).matches(String.class);
    }

    @Test
    public void automatically_resolves_types_of_throws_declarations() {
        @SuppressWarnings({"unused", "RedundantThrows"})
        class Origin {
            void call() throws InterruptedException {
            }
        }

        ThrowsDeclaration<?> throwsDeclaration = getOnlyElement(new ClassFileImporter().importClass(Origin.class).getThrowsDeclarations());

        assertThat(throwsDeclaration.getRawType()).isFullyImported(true);
        assertThatType(throwsDeclaration.getRawType()).matches(InterruptedException.class);
    }

    @Test
    public void automatically_resolves_array_component_types() {
        @SuppressWarnings("unused")
        class Origin {
            String[] oneDim;

            File[][] twoDim;
        }

        JavaClass javaClass = new ClassFileImporter().importClass(Origin.class);

        JavaClass componentType = javaClass.getField("oneDim").getRawType().getComponentType();
        assertThat(componentType).isFullyImported(true);
        assertThatType(componentType).matches(String.class);

        componentType = javaClass.getField("twoDim").getRawType().getComponentType().getComponentType();
        assertThat(componentType).isFullyImported(true);
        assertThatType(componentType).matches(File.class);
    }

    @MetaAnnotatedAnnotation
    private static class MetaAnnotatedClass {
    }

    @SuppressWarnings("unused")
    private @interface MetaAnnotationWithParameters {
        SomeAnnotationEnum someEnum();

        SomeAnnotationEnum someEnumDefault() default SomeAnnotationEnum.VARIABLE;

        ParameterAnnotation parameterAnnotation();

        ParameterAnnotation parameterAnnotationDefault() default @ParameterAnnotation(SomeAnnotationDefaultParameterType.class);
    }

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
            someEnum = SomeAnnotationEnum.CONSTANT,
            parameterAnnotation = @ParameterAnnotation(SomeAnnotationParameterType.class)
    )
    @SomeMetaAnnotation
    private @interface MetaAnnotatedAnnotation {
    }

    private enum SomeAnnotationEnum {
        CONSTANT,
        VARIABLE
    }

    private static class SomeAnnotationDefaultParameterType {
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
