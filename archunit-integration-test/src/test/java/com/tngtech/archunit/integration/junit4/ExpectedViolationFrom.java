package com.tngtech.archunit.integration.junit4;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Take the configuration of {@link com.tngtech.archunit.junit.ExpectedViolation} from the specified location#method.
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface ExpectedViolationFrom {
    Class<?> location();

    String method();
}
