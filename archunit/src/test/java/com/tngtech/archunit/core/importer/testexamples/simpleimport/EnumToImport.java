package com.tngtech.archunit.core.importer.testexamples.simpleimport;

@SuppressWarnings("unused")
public enum EnumToImport {
    FIRST, SECOND;

    public static EnumToImport someStaticField;

    EnumToImport instantField;
}
