package com.tngtech.archunit.example.cycles.complexcycles.slice2;

import com.tngtech.archunit.example.cycles.complexcycles.slice3.InheritedClassInSliceThree;
import com.tngtech.archunit.example.cycles.complexcycles.slice4.ClassWithAccessedFieldCallingMethodInSliceOne;

public class SliceTwoInheritingFromSliceThreeAndAccessingFieldInSliceFour extends InheritedClassInSliceThree {
    void accessSliceFour() {
        new ClassWithAccessedFieldCallingMethodInSliceOne().accessedField = "accessed";
    }
}
