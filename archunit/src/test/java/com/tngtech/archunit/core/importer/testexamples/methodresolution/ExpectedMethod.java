package com.tngtech.archunit.core.importer.testexamples.methodresolution;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
public @interface ExpectedMethod {
}
