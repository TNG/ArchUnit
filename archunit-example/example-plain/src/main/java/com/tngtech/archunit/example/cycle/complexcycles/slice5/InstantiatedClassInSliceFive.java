package com.tngtech.archunit.example.cycle.complexcycles.slice5;

import com.tngtech.archunit.example.cycle.complexcycles.slice1.ClassBeingCalledInSliceOne;

public class InstantiatedClassInSliceFive {
    void callSliceOne() {
        new ClassBeingCalledInSliceOne().doSomethingInSliceOne();
    }
}
