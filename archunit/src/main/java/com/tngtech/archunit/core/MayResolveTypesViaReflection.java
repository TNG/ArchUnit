package com.tngtech.archunit.core;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Marks the methods or classes, where resolving types via reflection is allowed.
 * These should be carefully controlled, because the import of classes may not rely on class loading
 * (while resolving dependencies might be done using the classpath), and neither should
 * the predefined conditions and predicates.
 */
@Retention(CLASS)
public @interface MayResolveTypesViaReflection {
    String reason();
}
