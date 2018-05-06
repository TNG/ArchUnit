package com.tngtech.archunit.example.cycle.constructorcycle.slice1;

import com.tngtech.archunit.example.cycle.constructorcycle.slice2.SliceTwoCallingConstructorInSliceOne;

public class SliceOneCallingConstructorInSliceTwo {
    void callSliceTwo() {
        new SliceTwoCallingConstructorInSliceOne();
    }
}
