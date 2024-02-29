package com.tngtech.archunit.core.importer.testexamples.typecast;

public class TypeCastInConstructor {
    @SuppressWarnings("unused")
    private TypeCastTestedType value;

    public TypeCastInConstructor(Object param) {
        value = (TypeCastTestedType) param;
    }
}
