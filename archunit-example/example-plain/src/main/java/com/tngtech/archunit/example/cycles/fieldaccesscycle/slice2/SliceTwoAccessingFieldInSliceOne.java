package com.tngtech.archunit.example.cycles.fieldaccesscycle.slice2;

import com.tngtech.archunit.example.cycles.fieldaccesscycle.slice1.ClassInSliceOneWithAccessedField;

public class SliceTwoAccessingFieldInSliceOne {
    private ClassInSliceOneWithAccessedField classInSliceOne;
    public String accessedField;

    void accessSliceOne() {
        classInSliceOne.accessedField = "accessed";
    }
}
