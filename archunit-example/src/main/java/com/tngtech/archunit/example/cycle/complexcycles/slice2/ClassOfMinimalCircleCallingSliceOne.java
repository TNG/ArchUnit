package com.tngtech.archunit.example.cycle.complexcycles.slice2;

import com.tngtech.archunit.example.cycle.complexcycles.slice1.ClassOfMinimalCircleCallingSliceTwo;

public class ClassOfMinimalCircleCallingSliceOne {
    private ClassOfMinimalCircleCallingSliceTwo classInSliceOne;

    public void callSliceOne() {
        classInSliceOne.callSliceTwo();
    }
}
