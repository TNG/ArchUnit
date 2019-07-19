package com.tngtech.archunit.example.cycles.simplecycle.slice3;

import com.tngtech.archunit.example.cycles.simplecycle.slice1.SomeClassBeingCalledInSliceOne;

public class SliceThreeCallingMethodOfSliceOne {
    private SomeClassBeingCalledInSliceOne someClassInSliceOne;

    void callSliceOne() {
        someClassInSliceOne.doSomethingInSliceOne();
    }

    public void doSomethingInSliceThree() {
    }
}
