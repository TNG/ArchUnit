package com.tngtech.archunit.example.cycles.inheritancecycle.slice1;

import com.tngtech.archunit.example.cycles.inheritancecycle.slice3.ClassThatImplementsInterfaceFromSliceOne;

public class ClassThatCallSliceThree {
    public ClassThatCallSliceThree() {
        new ClassThatImplementsInterfaceFromSliceOne();
    }
}
