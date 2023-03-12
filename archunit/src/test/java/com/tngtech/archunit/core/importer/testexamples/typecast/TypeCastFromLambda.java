package com.tngtech.archunit.core.importer.testexamples.typecast;

import java.io.File;
import java.io.FilterInputStream;
import java.util.function.Function;
import java.util.function.Supplier;

public class TypeCastFromLambda {
    Function<?, ?> reference() {
        return (object) -> (FilterInputStream) object;
    }
    
    Function<?, ?> referenceUsingMethodReference() {
        return FilterInputStream.class::cast; // Not a cast, but a method reference which does a type cast
    }

    Supplier<Function<?, ?>> nestedReference() {
        return () -> (object) -> (File) object;
    }
}
