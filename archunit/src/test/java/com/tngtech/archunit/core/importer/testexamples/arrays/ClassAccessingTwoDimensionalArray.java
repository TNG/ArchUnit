package com.tngtech.archunit.core.importer.testexamples.arrays;

@SuppressWarnings({"WeakerAccess", "unused"})
public class ClassAccessingTwoDimensionalArray {
    ClassUsedInArray[][] array;

    ClassUsedInArray access() {
        return array[1][1];
    }
}
