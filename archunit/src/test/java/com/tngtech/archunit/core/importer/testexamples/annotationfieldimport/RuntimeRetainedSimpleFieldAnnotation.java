package com.tngtech.archunit.core.importer.testexamples.annotationfieldimport;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(FIELD)
@Retention(RUNTIME)
public @interface RuntimeRetainedSimpleFieldAnnotation {
}
