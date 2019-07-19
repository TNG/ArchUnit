package com.tngtech.archunit.example.cycles.membercycle.slice2;

import com.tngtech.archunit.example.cycles.membercycle.slice3.SliceThreeWithMethodReturnTypeInSliceFour;

public class SliceTwoWithMethodParameterTypeInSliceThree {
    public void methodWithParameterInSliceThree(SliceThreeWithMethodReturnTypeInSliceFour methodParameter) {
    }
}
