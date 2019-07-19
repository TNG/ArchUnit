package com.tngtech.archunit.example.cycles.constructorcycle.slice2;

import com.tngtech.archunit.example.cycles.constructorcycle.slice1.SomeClassWithCalledConstructor;

public class SliceTwoCallingConstructorInSliceOne {
    void callSliceOne() {
        new SomeClassWithCalledConstructor();
    }

    void doSomethingInSliceTwo() {
    }
}
