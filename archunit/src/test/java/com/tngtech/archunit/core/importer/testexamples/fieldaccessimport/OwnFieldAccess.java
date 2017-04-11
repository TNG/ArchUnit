package com.tngtech.archunit.core.importer.testexamples.fieldaccessimport;

public class OwnFieldAccess {
    public String stringValue;
    public int intValue;

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public int getIntValue() {
        return intValue;
    }

    public void setIntValue(int intValue) {
        this.intValue = intValue;
    }
}
