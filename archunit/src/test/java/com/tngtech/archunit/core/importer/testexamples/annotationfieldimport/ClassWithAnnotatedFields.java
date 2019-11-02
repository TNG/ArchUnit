package com.tngtech.archunit.core.importer.testexamples.annotationfieldimport;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;

import com.tngtech.archunit.core.importer.testexamples.SomeAnnotation;
import com.tngtech.archunit.core.importer.testexamples.SomeEnum;

import static com.tngtech.archunit.core.importer.testexamples.SomeEnum.OTHER_VALUE;
import static com.tngtech.archunit.core.importer.testexamples.SomeEnum.SOME_VALUE;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class ClassWithAnnotatedFields {
    @FieldAnnotationWithStringValue("something")
    public Object stringAnnotatedField;

    @FieldAnnotationWithStringValue("otherThing")
    @FieldAnnotationWithIntValue(otherValue = "overridden")
    public Object stringAndIntAnnotatedField;

    @FieldAnnotationWithEnumClassAndArrayValue(
            value = OTHER_VALUE,
            enumArray = {SOME_VALUE, OTHER_VALUE},
            subAnnotation = @SubAnnotation("changed"),
            subAnnotationArray = {@SubAnnotation("another"), @SubAnnotation("one")},
            clazz = Map.class,
            classes = {Object.class, Serializable.class})
    public Object enumAndArrayAnnotatedField;

    @FieldAnnotationWithArrays(
            primitives = {},
            objects = {},
            enums = {},
            classes = {},
            annotations = {}
    )
    public Object fieldAnnotatedWithEmptyArrays;

    @SomeAnnotation(mandatory = "mandatory", mandatoryEnum = SOME_VALUE)
    public Object fieldAnnotatedWithAnnotationFromParentPackage;

    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface FieldAnnotationWithStringValue {
        String value();
    }

    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface FieldAnnotationWithIntValue {
        int intValue() default 0;

        String otherValue() default "Nothing";
    }

    @Target(FIELD)
    @Retention(RUNTIME)
    public @interface FieldAnnotationWithEnumClassAndArrayValue {
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

    public @interface SomeValueAnnotation {
        SomeEnum value();
    }
}
