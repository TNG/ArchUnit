package com.tngtech.archunit.core.importer.testexamples.annotationfieldimport;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.CLASS;

@Target(FIELD)
@Retention(CLASS)
public @interface ClassRetainedSimpleFieldAnnotation {
}
