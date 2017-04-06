package com.tngtech.archunit.junit;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.tngtech.archunit.lang.ArchRule;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a field of type {@link ArchRule} to be evaluated by the {@link ArchUnitRunner}.
 *
 * @see ArchUnitRunner
 */
@Target({FIELD, METHOD})
@Retention(RUNTIME)
public @interface ArchTest {
}
