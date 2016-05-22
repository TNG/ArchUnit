package com.tngtech.archunit.example.cycle.fieldaccesscycle.slice1;

import com.tngtech.archunit.example.cycle.fieldaccesscycle.slice2.SliceTwoAccessingFieldInSliceOne;

public class SliceOneAccessingFieldInSliceTwo {
    private SliceTwoAccessingFieldInSliceOne classInSliceTwo;

    void accessSliceTwo() {
        classInSliceTwo.accessedField = "accessed";
    }
}
