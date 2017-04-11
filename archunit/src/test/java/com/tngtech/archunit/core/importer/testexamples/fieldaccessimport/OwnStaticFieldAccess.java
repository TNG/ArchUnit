package com.tngtech.archunit.core.importer.testexamples.fieldaccessimport;

public class OwnStaticFieldAccess {
    static String staticStringValue;

    public static String getStaticStringValue() {
        return staticStringValue;
    }

    public static void setStaticStringValue(String staticStringValue) {
        OwnStaticFieldAccess.staticStringValue = staticStringValue;
    }
}
