package com.tngtech.archunit.core.importer.testexamples.fieldaccessimport;

import com.tngtech.archunit.core.importer.testexamples.complexexternal.ChildClass;

public class ExternalShadowedFieldAccess {
    String accessField() {
        return new ChildClass().someField;
    }
}
