package com.tngtech.archunit.example.cycles.constructorcycle.slice1;

import com.tngtech.archunit.example.cycles.constructorcycle.slice2.SliceTwoCallingConstructorInSliceOne;

public class SliceOneCallingConstructorInSliceTwo {
    void callSliceTwo() {
        new SliceTwoCallingConstructorInSliceOne();
    }
}
