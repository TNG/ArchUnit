package com.tngtech.archunit.core.importer.testexamples.arrays;

public class ClassAccessingOneDimensionalArray {
    ClassUsedInArray[] array;

    ClassUsedInArray access() {
        return array[1];
    }
}
