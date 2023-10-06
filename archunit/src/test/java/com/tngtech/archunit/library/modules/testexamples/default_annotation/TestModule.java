package com.tngtech.archunit.library.modules.testexamples.default_annotation;

public @interface TestModule {
    String name();

    String[] allowedDependencies() default {};

    String[] exposedPackages() default {};
}
