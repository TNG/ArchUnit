package com.tngtech.archunit.core.importer.testexamples.annotatedclassimport;

import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.List;

import com.tngtech.archunit.core.importer.testexamples.SomeEnum;

import static com.tngtech.archunit.core.importer.testexamples.SomeEnum.OTHER_VALUE;
import static com.tngtech.archunit.core.importer.testexamples.SomeEnum.SOME_VALUE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(TYPE)
@Retention(RUNTIME)
public @interface TypeAnnotationWithEnumAndArrayValue {
    SomeEnum value();

    SomeEnum valueWithDefault() default SOME_VALUE;

    SomeEnum[] enumArray();

    SomeEnum[] enumArrayWithDefault() default {OTHER_VALUE};

    SimpleAnnotation subAnnotation();

    SimpleAnnotation subAnnotationWithDefault() default @SimpleAnnotation("default");

    SimpleAnnotation[] subAnnotationArray();

    SimpleAnnotation[] subAnnotationArrayWithDefault() default {@SimpleAnnotation("first"), @SimpleAnnotation("second")};

    Class<?> clazz();

    Class<?> clazzWithDefault() default String.class;

    Class<?>[] classes();

    Class<?>[] classesWithDefault() default {Serializable.class, List.class};
}
