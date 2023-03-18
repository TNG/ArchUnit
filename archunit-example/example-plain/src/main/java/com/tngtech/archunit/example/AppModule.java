package com.tngtech.archunit.example;

public @interface AppModule {
    String name();

    String[] allowedDependencies() default {};

    String[] exposedPackages() default {};
}
