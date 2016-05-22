package com.tngtech.archunit.core.testexamples.fieldaccessimport;

import com.tngtech.archunit.core.testexamples.complexexternal.ChildClass;

public class ExternalShadowedFieldAccess {
    String accessField() {
        return new ChildClass().someField;
    }
}
