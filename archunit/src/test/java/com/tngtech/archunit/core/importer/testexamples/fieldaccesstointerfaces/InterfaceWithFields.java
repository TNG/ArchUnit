package com.tngtech.archunit.core.importer.testexamples.fieldaccesstointerfaces;

// NOTE: The compiler will inline Strings or primitives, thus use field type Object
public interface InterfaceWithFields extends ParentInterfaceWithFields {
    Object objectFieldOne = "objectFieldOne";
    Object objectFieldTwo = "objectFieldTwo";
}
