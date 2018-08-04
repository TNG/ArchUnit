package com.tngtech.archunit.example.cycle.membercycle.slice4;

import com.tngtech.archunit.example.cycle.membercycle.slice1.SliceOneWithFieldTypeInSliceTwo;

public class SliceFourWithConstructorParameterInSliceOne {
    public SliceFourWithConstructorParameterInSliceOne(SliceOneWithFieldTypeInSliceTwo fieldInSliceTwo) {
    }
}
