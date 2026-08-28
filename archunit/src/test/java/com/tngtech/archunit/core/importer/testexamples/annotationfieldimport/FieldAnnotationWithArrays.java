package com.tngtech.archunit.core.importer.testexamples.annotationfieldimport;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.tngtech.archunit.core.importer.testexamples.SomeEnum;
import com.tngtech.archunit.core.importer.testexamples.annotatedclassimport.SimpleAnnotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(ElementType.FIELD)
@Retention(RUNTIME)
public @interface FieldAnnotationWithArrays {
    int[] primitives();

    String[] objects();

    SomeEnum[] enums();

    Class<?>[] classes();

    SimpleAnnotation[] annotations();
}
