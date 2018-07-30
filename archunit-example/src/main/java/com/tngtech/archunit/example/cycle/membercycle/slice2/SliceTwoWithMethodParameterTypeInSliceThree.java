package com.tngtech.archunit.example.cycle.membercycle.slice2;

import com.tngtech.archunit.example.cycle.membercycle.slice3.SliceThreeWithMethodReturnTypeInSliceFour;

public class SliceTwoWithMethodParameterTypeInSliceThree {
    public void methodWithParameterInSliceThree(SliceThreeWithMethodReturnTypeInSliceFour methodParameter) {
    }
}
