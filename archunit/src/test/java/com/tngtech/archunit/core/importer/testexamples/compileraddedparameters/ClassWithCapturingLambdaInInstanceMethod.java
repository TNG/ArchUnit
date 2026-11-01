package com.tngtech.archunit.core.importer.testexamples.compileraddedparameters;

import java.util.function.Supplier;

@SuppressWarnings("unused")
public class ClassWithCapturingLambdaInInstanceMethod {
    public Supplier<String> supplier(String captured) {
        return () -> captured;
    }
}
