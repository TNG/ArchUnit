package com.tngtech.archunit.example.cycle.complexcycles.slice6;

import com.tngtech.archunit.example.cycle.complexcycles.slice1.ClassBeingCalledInSliceOne;
import com.tngtech.archunit.example.cycle.complexcycles.slice2.InstantiatedClassInSliceTwo;
import com.tngtech.archunit.example.cycle.complexcycles.slice4.ClassWithAccessedFieldCallingMethodInSliceOne;

public class UnproblematicClassInSliceSix {
    void callSliceOneTwoAndFour() {
        new ClassBeingCalledInSliceOne();
        new InstantiatedClassInSliceTwo();
        new ClassWithAccessedFieldCallingMethodInSliceOne().accessedField = "accessed";
    }
}
