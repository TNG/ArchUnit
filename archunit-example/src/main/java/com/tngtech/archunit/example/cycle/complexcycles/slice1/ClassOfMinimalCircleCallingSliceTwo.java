package com.tngtech.archunit.example.cycle.complexcycles.slice1;

import com.tngtech.archunit.example.cycle.complexcycles.slice2.ClassOfMinimalCircleCallingSliceOne;

public class ClassOfMinimalCircleCallingSliceTwo {
    private ClassOfMinimalCircleCallingSliceOne classInSliceTwo;

    public void callSliceTwo() {
        classInSliceTwo.callSliceOne();
    }
}
