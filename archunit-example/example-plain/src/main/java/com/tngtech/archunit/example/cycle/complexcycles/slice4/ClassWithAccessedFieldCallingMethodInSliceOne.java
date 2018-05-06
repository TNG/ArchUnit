package com.tngtech.archunit.example.cycle.complexcycles.slice4;

import com.tngtech.archunit.example.cycle.complexcycles.slice1.ClassBeingCalledInSliceOne;

public class ClassWithAccessedFieldCallingMethodInSliceOne {
    private ClassBeingCalledInSliceOne classInSliceOne;
    public String accessedField;

    void callSliceOne() {
        classInSliceOne.doSomethingInSliceOne();
    }
}
