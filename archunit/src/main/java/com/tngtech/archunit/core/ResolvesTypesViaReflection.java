package com.tngtech.archunit.core;

import java.lang.annotation.Retention;

import com.tngtech.archunit.Internal;

import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Marks the methods or classes, that resolve types via reflection so we can use it for architecture
 * assertions.
 */
@Retention(CLASS)
@Internal
public @interface ResolvesTypesViaReflection {
}
