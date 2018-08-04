package com.tngtech.archunit.example.cycle.inheritancecycle.slice1;

import com.tngtech.archunit.example.cycle.inheritancecycle.slice3.ClassThatImplementsInterfaceFromSliceOne;

public class ClassThatCallSliceThree {
    public ClassThatCallSliceThree() {
        new ClassThatImplementsInterfaceFromSliceOne();
    }
}
