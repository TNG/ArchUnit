package com.tngtech.archunit.core.importer.testexamples.typecast;

@SuppressWarnings({"unused", "ConstantConditions"})
public class TypeCastInStaticInitializer {
    static {
        Object foo = new TypeCastTestedType();
        TypeCastTestedType bar = (TypeCastTestedType) foo;
    }
}
