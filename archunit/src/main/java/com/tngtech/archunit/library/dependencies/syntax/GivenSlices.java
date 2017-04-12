package com.tngtech.archunit.library.dependencies.syntax;

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface GivenSlices extends GivenNamedSlices {
    @PublicAPI(usage = ACCESS)
    GivenNamedSlices namingSlices(String pattern);
}