package com.tngtech.archunit.core.testexamples.fieldaccesstointerfaces;

public class ClassAccessingInterfaceFields {
    private ClassWithInterfacesWithFields classWithInterfacesWithFields;

    void access() {
        accept("" + classWithInterfacesWithFields.objectFieldOne);
        accept("" + classWithInterfacesWithFields.objectFieldTwo);
        accept("" + classWithInterfacesWithFields.otherObjectFieldOne);
        accept("" + classWithInterfacesWithFields.otherObjectFieldTwo);
        accept("" + classWithInterfacesWithFields.parentObjectFieldOne);
        accept("" + classWithInterfacesWithFields.parentObjectFieldTwo);
    }

    private void accept(String string) {
    }
}
