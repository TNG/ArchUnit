package com.tngtech.archunit.core.importer.testexamples.typeannotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * RUNTIME-retained annotation that targets both {@link java.lang.annotation.ElementType#FIELD FIELD}
 * and {@link java.lang.annotation.ElementType#TYPE_USE TYPE_USE}. Used to verify that the importer
 * disambiguates a single {@code @...} occurrence into a declaration annotation on the field
 * <em>and</em> a type annotation on the field's type.
 */
@Target({FIELD, TYPE_USE})
@Retention(RUNTIME)
public @interface RuntimeRetainedFieldAndTypeUseAnnotation {
}
