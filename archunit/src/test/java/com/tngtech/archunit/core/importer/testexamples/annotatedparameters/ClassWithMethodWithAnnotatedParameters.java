package com.tngtech.archunit.core.importer.testexamples.annotatedparameters;

import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tngtech.archunit.core.importer.testexamples.SomeEnum;

import static com.tngtech.archunit.core.importer.testexamples.SomeEnum.OTHER_VALUE;
import static com.tngtech.archunit.core.importer.testexamples.SomeEnum.SOME_VALUE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@SuppressWarnings("unused")
public class ClassWithMethodWithAnnotatedParameters {
    public static String methodWithTwoUnannotatedParameters = "methodWithTwoUnannotatedParameters";
    public static String methodWithOneAnnotatedParameterWithOneAnnotation = "methodWithOneAnnotatedParameterWithOneAnnotation";
    public static String methodWithOneAnnotatedParameterWithTwoAnnotations = "methodWithOneAnnotatedParameterWithTwoAnnotations";
    public static String methodWithTwoAnnotatedParameters = "methodWithTwoAnnotatedParameters";
    public static String methodWithAnnotatedParametersGap = "methodWithAnnotatedParametersGap";

    void methodWithTwoUnannotatedParameters(String one, int two) {
    }

    void methodWithOneAnnotatedParameterWithOneAnnotation(
            @SomeParameterAnnotation(
                    value = OTHER_VALUE,
                    enumArray = {SOME_VALUE, OTHER_VALUE},
                    subAnnotation = @SimpleParameterAnnotation("changed"),
                    subAnnotationArray = {@SimpleParameterAnnotation("one"), @SimpleParameterAnnotation("two")},
                    clazz = Map.class,
                    classes = {Object.class, Serializable.class}
            ) String param
    ) {
    }

    void methodWithOneAnnotatedParameterWithTwoAnnotations(
            @OtherParameterAnnotation(String.class)
            @SomeParameterAnnotation(
                    value = OTHER_VALUE,
                    enumArray = {SOME_VALUE, OTHER_VALUE},
                    subAnnotation = @SimpleParameterAnnotation("changed"),
                    subAnnotationArray = {@SimpleParameterAnnotation("one"), @SimpleParameterAnnotation("two")},
                    clazz = Map.class,
                    classes = {Object.class, Serializable.class}
            ) String param
    ) {
    }

    void methodWithTwoAnnotatedParameters(
            @SomeParameterAnnotation(
                    value = OTHER_VALUE,
                    enumArray = {SOME_VALUE, OTHER_VALUE},
                    subAnnotation = @SimpleParameterAnnotation("first"),
                    subAnnotationArray = {@SimpleParameterAnnotation("first_one"), @SimpleParameterAnnotation("first_two")},
                    clazz = String.class,
                    classes = {String.class, Serializable.class}
            ) String first,
            @OtherParameterAnnotation(String.class)
            @SomeParameterAnnotation(
                    value = SOME_VALUE,
                    enumArray = {OTHER_VALUE, SOME_VALUE},
                    subAnnotation = @SimpleParameterAnnotation("second"),
                    subAnnotationArray = {@SimpleParameterAnnotation("second_one"), @SimpleParameterAnnotation("second_two")},
                    clazz = List.class,
                    classes = {Set.class, Map.class}
            ) int second
    ) {
    }

    <T> void methodWithAnnotatedParametersGap(
            @SimpleParameterAnnotation("first") String first,
            int second,
            T third,
            @SimpleParameterAnnotation("fourth") List<String> fourth
    ) {
    }

    @Target(ElementType.PARAMETER)
    @Retention(RUNTIME)
    public @interface SomeParameterAnnotation {
        SomeEnum value();

        SomeEnum valueWithDefault() default SOME_VALUE;

        SomeEnum[] enumArray();

        SomeEnum[] enumArrayWithDefault() default {OTHER_VALUE};

        SimpleParameterAnnotation subAnnotation();

        SimpleParameterAnnotation subAnnotationWithDefault() default @SimpleParameterAnnotation("default");

        SimpleParameterAnnotation[] subAnnotationArray();

        SimpleParameterAnnotation[] subAnnotationArrayWithDefault() default {@SimpleParameterAnnotation("first"), @SimpleParameterAnnotation("second")};

        Class<?> clazz();

        Class<?> clazzWithDefault() default String.class;

        Class<?>[] classes();

        Class<?>[] classesWithDefault() default {Serializable.class, List.class};
    }

    @Target(ElementType.PARAMETER)
    @Retention(RUNTIME)
    public @interface SimpleParameterAnnotation {
        String value();
    }

    @Target(ElementType.PARAMETER)
    @Retention(RUNTIME)
    public @interface OtherParameterAnnotation {
        Class<?> value();
    }
}
