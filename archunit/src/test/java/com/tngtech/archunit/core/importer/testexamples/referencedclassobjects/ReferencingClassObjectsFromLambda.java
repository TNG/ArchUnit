package com.tngtech.archunit.core.importer.testexamples.referencedclassobjects;

import java.io.File;
import java.io.FilterInputStream;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class ReferencingClassObjectsFromLambda {
    Supplier<Class<?>> reference() {
        return () -> FilterInputStream.class;
    }

    Supplier<Supplier<Class<?>>> nestedReference() {
        return () -> () -> File.class;
    }
}
