package com.tngtech.archunit.library.dependencies.syntax;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.domain.properties.CanOverrideDescription;
import com.tngtech.archunit.lang.syntax.elements.GivenObjects;
import com.tngtech.archunit.library.dependencies.Slice;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface GivenNamedSlices extends GivenObjects<Slice>, CanOverrideDescription<GivenNamedSlices> {
    @PublicAPI(usage = ACCESS)
    SlicesShould should();
}