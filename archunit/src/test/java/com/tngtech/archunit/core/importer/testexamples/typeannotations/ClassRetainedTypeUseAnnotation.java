package com.tngtech.archunit.core.importer.testexamples.typeannotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.CLASS;

@Target(TYPE_USE)
@Retention(CLASS)
public @interface ClassRetainedTypeUseAnnotation {
}
