package com.tngtech.archunit.junit;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Equivalent to {@link org.junit.Ignore}, but can be applied to fields to mark rules annotated with @{@link ArchTest}
 * to be ignored by the {@link ArchUnitRunner}.
 */
@Target({TYPE, FIELD})
@Retention(RUNTIME)
public @interface ArchIgnore {
}
