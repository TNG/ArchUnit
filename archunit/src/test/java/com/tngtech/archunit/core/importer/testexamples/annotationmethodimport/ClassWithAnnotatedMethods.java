package com.tngtech.archunit.core.importer.testexamples.annotationmethodimport;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;

import com.tngtech.archunit.core.importer.testexamples.SomeAnnotation;
import com.tngtech.archunit.core.importer.testexamples.SomeEnum;

import static com.tngtech.archunit.core.importer.testexamples.SomeEnum.OTHER_VALUE;
import static com.tngtech.archunit.core.importer.testexamples.SomeEnum.SOME_VALUE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class ClassWithAnnotatedMethods {
    public static final String stringAnnotatedMethod = "stringAnnotatedMethod";
    public static final String stringAndIntAnnotatedMethod = "stringAndIntAnnotatedMethod";
    public static final String enumAndArrayAnnotatedMethod = "enumAndArrayAnnotatedMethod";
    public static final String methodAnnotatedWithEmptyArrays = "methodAnnotatedWithEmptyArrays";
    public static final String methodAnnotatedWithAnnotationFromParentPackage =
            "methodAnnotatedWithAnnotationFromParentPackage";

    @MethodAnnotationWithEnumAndArrayValue(
            value = OTHER_VALUE,
            enumArray = {SOME_VALUE, OTHER_VALUE},
            subAnnotation = @SubAnnotation("changed"),
            subAnnotationArray = {@SubAnnotation("another"), @SubAnnotation("one")},
            clazz = Map.class,
            classes = {Object.class, Serializable.class})
    public ClassWithAnnotatedMethods() {

    }

    @MethodAnnotationWithStringValue("something")
    public Object stringAnnotatedMethod() {
        return null;
    }

    @MethodAnnotationWithStringValue("otherThing")
    @MethodAnnotationWithIntValue(intValue = 8, otherValue = "overridden")
    public Object stringAndIntAnnotatedMethod() {
        return null;
    }

    @MethodAnnotationWithEnumAndArrayValue(
            value = OTHER_VALUE,
            enumArray = {SOME_VALUE, OTHER_VALUE},
            subAnnotation = @SubAnnotation("changed"),
            subAnnotationArray = {@SubAnnotation("another"), @SubAnnotation("one")},
            clazz = Map.class,
            classes = {Object.class, Serializable.class})
    public Object enumAndArrayAnnotatedMethod() {
        return null;
    }

    @MethodAnnotationWithArrays(
            primitives = {},
            objects = {},
            enums = {},
            classes = {},
            annotations = {})
    public void methodAnnotatedWithEmptyArrays() {
    }

    @SomeAnnotation(mandatory = "mandatory", mandatoryEnum = SOME_VALUE)
    public void methodAnnotatedWithAnnotationFromParentPackage() {
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    public @interface MethodAnnotationWithStringValue {
        String value();
    }

    @Target(METHOD)
    @Retention(RUNTIME)
    public @interface MethodAnnotationWithIntValue {
        int intValue();

        int intValueWithDefault() default 0;

        String otherValue();

        String otherValueWithDefault() default "Nothing";
    }

    @Target({METHOD, CONSTRUCTOR})
    @Retention(RUNTIME)
    public @interface MethodAnnotationWithEnumAndArrayValue {
        SomeEnum value();

        SomeEnum valueWithDefault() default SOME_VALUE;

        SomeEnum[] enumArray();

        SomeEnum[] enumArrayWithDefault() default {OTHER_VALUE};

        SubAnnotation subAnnotation();

        SubAnnotation subAnnotationWithDefault() default @SubAnnotation("default");

        SubAnnotation[] subAnnotationArray();

        SubAnnotation[] subAnnotationArrayWithDefault() default {@SubAnnotation("first"), @SubAnnotation("second")};

        Class<?> clazz();

        Class<?> clazzWithDefault() default String.class;

        Class<?>[] classes();

        Class<?>[] classesWithDefault() default {Serializable.class, List.class};
    }

    public @interface SubAnnotation {
        String value();
    }
}
