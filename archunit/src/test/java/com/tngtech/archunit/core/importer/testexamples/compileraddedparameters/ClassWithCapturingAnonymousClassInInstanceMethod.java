package com.tngtech.archunit.core.importer.testexamples.compileraddedparameters;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public class ClassWithCapturingAnonymousClassInInstanceMethod {
    public Supplier<String> supplier(String captured) {
        return new Supplier<String>() {
            @Override
            public String get() {
                return captured;
            }
        };
    }
}
