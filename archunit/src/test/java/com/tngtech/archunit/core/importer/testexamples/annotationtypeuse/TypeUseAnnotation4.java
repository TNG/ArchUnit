package com.tngtech.archunit.core.importer.testexamples.annotationtypeuse;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Third RUNTIME-retained TYPE_USE annotation, used together with
 * {@link TypeUseAnnotation}, {@link TypeUseAnnotation2}, and {@link TypeUseAnnotation3} to exercise scenarios that require four distinct
 * TYPE_USE annotation types applied at the same or at different positions on a type.
 */
@Target(TYPE_USE)
@Retention(RUNTIME)
public @interface TypeUseAnnotation4 {
}
