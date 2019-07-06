package com.tngtech.archunit.example.cycles.membercycle.slice4;

import com.tngtech.archunit.example.cycles.membercycle.slice1.SliceOneWithFieldTypeInSliceTwo;

public class SliceFourWithConstructorParameterInSliceOne {
    public SliceFourWithConstructorParameterInSliceOne(SliceOneWithFieldTypeInSliceTwo fieldInSliceTwo) {
    }
}
