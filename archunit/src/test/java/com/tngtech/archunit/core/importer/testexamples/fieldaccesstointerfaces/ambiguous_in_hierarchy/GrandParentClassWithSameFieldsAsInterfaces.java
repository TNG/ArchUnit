package com.tngtech.archunit.core.importer.testexamples.fieldaccesstointerfaces.ambiguous_in_hierarchy;

import com.tngtech.archunit.core.importer.testexamples.fieldaccesstointerfaces.InterfaceWithFields;

@SuppressWarnings({"unused"})
public class GrandParentClassWithSameFieldsAsInterfaces extends GreatGrandParentClass implements InterfaceWithFields {
    private final Object otherObjectFieldOne = "otherObjectFieldOne";
}
