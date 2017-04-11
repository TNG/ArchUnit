package com.tngtech.archunit.core.importer.testexamples.annotatedclassimport;

import java.lang.annotation.Retention;

import com.tngtech.archunit.core.importer.testexamples.SomeEnum;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
public @interface ClassAnnotationWithArrays {
    int[] primitives();

    String[] objects();

    SomeEnum[] enums();

    Class<?>[] classes();

    SimpleAnnotation[] annotations();
}
