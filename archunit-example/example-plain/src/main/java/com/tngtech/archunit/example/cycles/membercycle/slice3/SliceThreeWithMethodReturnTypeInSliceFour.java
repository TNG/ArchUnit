package com.tngtech.archunit.example.cycles.membercycle.slice3;

import com.tngtech.archunit.example.cycles.membercycle.slice4.SliceFourWithConstructorParameterInSliceOne;

public class SliceThreeWithMethodReturnTypeInSliceFour {
    public SliceFourWithConstructorParameterInSliceOne methodWithReturnTypeInSliceFour() {
        return null;
    }
}
