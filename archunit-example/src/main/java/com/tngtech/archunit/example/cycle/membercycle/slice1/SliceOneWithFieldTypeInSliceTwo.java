package com.tngtech.archunit.example.cycle.membercycle.slice1;

import com.tngtech.archunit.example.cycle.membercycle.slice2.SliceTwoWithMethodParameterTypeInSliceThree;

public class SliceOneWithFieldTypeInSliceTwo {
    public SliceTwoWithMethodParameterTypeInSliceThree classInSliceTwo;
}
