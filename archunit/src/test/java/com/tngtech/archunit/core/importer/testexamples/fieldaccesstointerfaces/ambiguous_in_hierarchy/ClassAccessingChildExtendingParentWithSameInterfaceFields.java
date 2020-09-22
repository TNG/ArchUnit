package com.tngtech.archunit.core.importer.testexamples.fieldaccesstointerfaces.ambiguous_in_hierarchy;

@SuppressWarnings({"ConstantConditions", "unused"})
public class ClassAccessingChildExtendingParentWithSameInterfaceFields {
    Object access(ChildExtendingParentWithSameInterfaceFields child) {
        return child.objectFieldOne != null ? child.objectFieldOne : child.otherObjectFieldOne;
    }
}
