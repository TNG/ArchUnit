package com.tngtech.archunit.example.cycles.simplecycle.slice2;

import com.tngtech.archunit.example.cycles.simplecycle.slice3.SliceThreeCallingMethodOfSliceOne;

public class SliceTwoCallingMethodOfSliceThree {
    private SliceThreeCallingMethodOfSliceOne classInSliceThree;

    void callSliceThree() {
        classInSliceThree.doSomethingInSliceThree();
    }

    public void doSomethingInSliceTwo() {
    }
}
