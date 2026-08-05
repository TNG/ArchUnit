package com.tngtech.archunit.core.importer.testexamples.typeannotations;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Second RUNTIME-retained TYPE_USE annotation, used together with
 * {@link RuntimeRetainedTypeUseAnnotation} to exercise scenarios that require two distinct
 * TYPE_USE annotation types applied at the same or at different positions on a type.
 */
@Target(TYPE_USE)
@Retention(RUNTIME)
public @interface SecondRuntimeRetainedTypeUseAnnotation {
}
