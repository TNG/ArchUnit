package com.tngtech.archunit.library.modules.testexamples;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
public @interface MyModule {
    String name();
}
