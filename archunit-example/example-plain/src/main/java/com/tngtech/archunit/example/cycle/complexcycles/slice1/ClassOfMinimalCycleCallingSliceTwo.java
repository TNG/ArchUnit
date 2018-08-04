package com.tngtech.archunit.example.cycle.complexcycles.slice1;

import com.tngtech.archunit.example.cycle.complexcycles.slice2.ClassOfMinimalCycleCallingSliceOne;

public class ClassOfMinimalCycleCallingSliceTwo {
    private ClassOfMinimalCycleCallingSliceOne classInSliceTwo;

    public void callSliceTwo() {
        classInSliceTwo.callSliceOne();
    }
}
