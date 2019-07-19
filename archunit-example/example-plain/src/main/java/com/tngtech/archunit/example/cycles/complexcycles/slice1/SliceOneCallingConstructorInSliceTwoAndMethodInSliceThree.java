package com.tngtech.archunit.example.cycles.complexcycles.slice1;

import com.tngtech.archunit.example.cycles.complexcycles.slice2.InstantiatedClassInSliceTwo;
import com.tngtech.archunit.example.cycles.complexcycles.slice3.ClassCallingConstructorInSliceFive;

public class SliceOneCallingConstructorInSliceTwoAndMethodInSliceThree {
    private ClassCallingConstructorInSliceFive classInSliceThree;

    void callSliceTwo() {
        new InstantiatedClassInSliceTwo();
    }

    void callSliceThree() {
        classInSliceThree.callSliceFive();
    }
}