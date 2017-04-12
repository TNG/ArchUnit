package com.tngtech.archunit.library.dependencies.syntax;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface SlicesShould {
    @PublicAPI(usage = ACCESS)
    ArchRule beFreeOfCycles();

    @PublicAPI(usage = ACCESS)
    ArchRule notDependOnEachOther();
}
