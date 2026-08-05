package com.tngtech.archunit.core.importer.testexamples.typeannotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(TYPE_USE)
@Retention(RUNTIME)
public @interface RuntimeRetainedTypeUseAnnotation {
}
