package com.tngtech.archunit.example.cycles.complexcycles.slice4;

import com.tngtech.archunit.example.cycles.complexcycles.slice1.ClassBeingCalledInSliceOne;

public class ClassWithAccessedFieldCallingMethodInSliceOne {
    private ClassBeingCalledInSliceOne classInSliceOne;
    public String accessedField;

    void callSliceOne() {
        classInSliceOne.doSomethingInSliceOne();
    }
}
