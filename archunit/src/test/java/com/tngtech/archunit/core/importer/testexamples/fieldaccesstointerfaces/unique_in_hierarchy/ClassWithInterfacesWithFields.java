package com.tngtech.archunit.core.importer.testexamples.fieldaccesstointerfaces.unique_in_hierarchy;

import com.tngtech.archunit.core.importer.testexamples.fieldaccesstointerfaces.InterfaceWithFields;
import com.tngtech.archunit.core.importer.testexamples.fieldaccesstointerfaces.OtherInterfaceWithFields;

public class ClassWithInterfacesWithFields implements InterfaceWithFields, OtherInterfaceWithFields {
}
