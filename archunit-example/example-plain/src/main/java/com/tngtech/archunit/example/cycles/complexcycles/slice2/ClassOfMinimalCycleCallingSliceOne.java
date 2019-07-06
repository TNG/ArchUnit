package com.tngtech.archunit.example.cycles.complexcycles.slice2;

import com.tngtech.archunit.example.cycles.complexcycles.slice1.ClassOfMinimalCycleCallingSliceTwo;

public class ClassOfMinimalCycleCallingSliceOne {
    private ClassOfMinimalCycleCallingSliceTwo classInSliceOne;

    public void callSliceOne() {
        classInSliceOne.callSliceTwo();
    }
}
