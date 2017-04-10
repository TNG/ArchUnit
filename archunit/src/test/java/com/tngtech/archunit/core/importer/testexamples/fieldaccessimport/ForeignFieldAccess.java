package com.tngtech.archunit.core.importer.testexamples.fieldaccessimport;

public class ForeignFieldAccess {
    public String getStringFromOther() {
        return new OwnFieldAccess().stringValue;
    }

    public void setStringFromOther() {
        new OwnFieldAccess().stringValue = "string";
    }

    public int getIntFromOther() {
        return new OwnFieldAccess().intValue;
    }

    public void setIntFromOther() {
        new OwnFieldAccess().intValue = 0;
    }
}
