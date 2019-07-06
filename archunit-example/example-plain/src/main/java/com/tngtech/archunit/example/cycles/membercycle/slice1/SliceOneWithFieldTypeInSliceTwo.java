package com.tngtech.archunit.example.cycles.membercycle.slice1;

import com.tngtech.archunit.example.cycles.membercycle.slice2.SliceTwoWithMethodParameterTypeInSliceThree;

public class SliceOneWithFieldTypeInSliceTwo {
    public SliceTwoWithMethodParameterTypeInSliceThree classInSliceTwo;
}
