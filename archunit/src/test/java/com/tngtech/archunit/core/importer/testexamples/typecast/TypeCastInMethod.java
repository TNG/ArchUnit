package com.tngtech.archunit.core.importer.testexamples.typecast;

@SuppressWarnings("unused")
public class TypeCastInMethod {
    TypeCastTestedType method(Object param) {
        return (TypeCastTestedType) param;
    }
}
