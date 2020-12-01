package com.tngtech.archunit.core.importer;

import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.HasAnnotations;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.testutil.Assertions.assertThatAnnotation;
import static com.tngtech.archunit.testutil.Assertions.assertThatType;
import static com.tngtech.archunit.testutil.assertion.JavaAnnotationAssertion.annotationProperty;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;

@RunWith(DataProviderRunner.class)
public class ClassFileImporterAnnotationsTest {

    @Test
    public void meta_annotation_types_are_transitively_imported() {
        JavaClass javaClass = new ClassFileImporter().importClass(MetaAnnotatedClass.class);
        JavaAnnotation<JavaClass> someAnnotation = javaClass.getAnnotationOfType(SomeAnnotation.class.getName());
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
                new ClassFileImporter().importClass(ClassWithMetaAnnotatedConstructor.class).getConstructor()
        );
    }

    @Test
    @UseDataProvider("elementsAnnotatedWithSomeAnnotation")
    public void parameters_of_meta_annotations_are_transitively_imported(HasAnnotations<?> annotatedWithSomeAnnotation) {
        JavaAnnotation<?> someAnnotation = annotatedWithSomeAnnotation
                .getAnnotationOfType(SomeAnnotation.class.getName());
        JavaAnnotation<?> metaAnnotationWithParameters = someAnnotation.getRawType()
                .getAnnotationOfType(MetaAnnotationWithParameters.class.getName());

        assertThatAnnotation(metaAnnotationWithParameters)
                .hasEnumProperty("someEnum", SomeEnum.CONSTANT)
                .hasEnumProperty("someEnumDefault", SomeEnum.VARIABLE)
                .hasAnnotationProperty("parameterAnnotation",
                        annotationProperty()
                                .withAnnotationType(ParameterAnnotation.class)
                                .withClassProperty("value", SomeAnnotationParameterType.class))
                .hasAnnotationProperty("parameterAnnotationDefault",
                        annotationProperty()
                                .withAnnotationType(ParameterAnnotation.class)
                                .withClassProperty("value", Integer.class));

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

    @SuppressWarnings("unused")
    private @interface MetaAnnotationWithParameters {
        SomeEnum someEnum();

        SomeEnum someEnumDefault() default SomeEnum.VARIABLE;

        ParameterAnnotation parameterAnnotation();

        ParameterAnnotation parameterAnnotationDefault() default @ParameterAnnotation(Integer.class);
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
            someEnum = SomeEnum.CONSTANT,
            parameterAnnotation = @ParameterAnnotation(SomeAnnotationParameterType.class)
    )
    @SomeMetaAnnotation
    private @interface SomeAnnotation {
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

    @SomeAnnotation
    private static class MetaAnnotatedClass {
    }

    @SuppressWarnings("unused")
    private static class ClassWithMetaAnnotatedField {
        @SomeAnnotation
        int metaAnnotatedField;
    }

    @SuppressWarnings("unused")
    private static class ClassWithMetaAnnotatedMethod {
        @SomeAnnotation
        void metaAnnotatedMethod() {
        }
    }

    @SuppressWarnings("unused")
    private static class ClassWithMetaAnnotatedConstructor {
        @SomeAnnotation
        ClassWithMetaAnnotatedConstructor() {
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
