package com.tngtech.archunit.core.importer.testexamples.fieldaccesstointerfaces.unique_in_hierarchy;

@SuppressWarnings("unused")
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
