package com.tngtech.archunit.core.importer.testexamples.fieldaccessimport;

public class ForeignStaticFieldAccess {
    public static String getStaticStringFromOther() {
        return OwnStaticFieldAccess.staticStringValue;
    }

    public static void setStaticStringFromOther() {
        OwnStaticFieldAccess.staticStringValue = "string";
    }
}
