package com.tngtech.archunit.example.cycles.simplecycle.slice1;

import com.tngtech.archunit.example.cycles.simplecycle.slice2.SliceTwoCallingMethodOfSliceThree;

public class SliceOneCallingMethodInSliceTwo {
    private SliceTwoCallingMethodOfSliceThree classInSliceTwo;

    void callSliceTwo() {
        classInSliceTwo.doSomethingInSliceTwo();
    }

    public void doSomethingInSliceOne() {
    }
}
