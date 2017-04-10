package com.tngtech.archunit.core.importer.testexamples.annotatedclassimport;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SimpleAnnotation {
    String value();
}