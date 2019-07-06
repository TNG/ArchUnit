package com.tngtech.archunit.example.cycles.complexcycles.slice1;

import com.tngtech.archunit.example.cycles.complexcycles.slice2.ClassOfMinimalCycleCallingSliceOne;

public class ClassOfMinimalCycleCallingSliceTwo {
    private ClassOfMinimalCycleCallingSliceOne classInSliceTwo;

    public void callSliceTwo() {
        classInSliceTwo.callSliceOne();
    }
}
