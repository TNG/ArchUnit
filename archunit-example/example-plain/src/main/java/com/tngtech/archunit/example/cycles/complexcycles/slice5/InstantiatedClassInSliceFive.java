package com.tngtech.archunit.example.cycles.complexcycles.slice5;

import com.tngtech.archunit.example.cycles.complexcycles.slice1.ClassBeingCalledInSliceOne;

public class InstantiatedClassInSliceFive {
    void callSliceOne() {
        new ClassBeingCalledInSliceOne().doSomethingInSliceOne();
    }
}
