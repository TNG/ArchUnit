package com.tngtech.archunit.core.testexamples.fieldaccessimport;

import com.tngtech.archunit.core.testexamples.complexexternal.ChildClass;
import com.tngtech.archunit.core.testexamples.fieldimport.ClassWithIntAndObjectFields;

public class ExternalFieldAccess {
    void access() {
        new ClassWithIntAndObjectFields().objectField = new Object();
    }

    Object accessInheritedExternalField() {
        return new ChildClass().someParentField;
    }
}
