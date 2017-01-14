package com.tngtech.archunit.core.testexamples.annotationfieldimport;

import java.lang.annotation.Retention;

import com.tngtech.archunit.core.testexamples.SomeEnum;
import com.tngtech.archunit.core.testexamples.annotatedclassimport.SimpleAnnotation;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
public @interface FieldAnnotationWithArrays {
    int[] primitives();

    String[] objects();

    SomeEnum[] enums();

    Class<?>[] classes();

    SimpleAnnotation[] annotations();
}
