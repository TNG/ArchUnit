package com.tngtech.archunit.core.importer.testexamples.instanceofcheck;

import java.io.File;
import java.io.FilterInputStream;
import java.util.function.Predicate;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class CheckingInstanceofFromLambda {
    Predicate<?> reference() {
        return (object) -> object instanceof FilterInputStream;
    }

    Supplier<Predicate<?>> nestedReference() {
        return () -> (object) -> object instanceof File;
    }
}
