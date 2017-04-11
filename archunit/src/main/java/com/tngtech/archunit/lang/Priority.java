package com.tngtech.archunit.lang;

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public enum Priority {
    @PublicAPI(usage = ACCESS)
    HIGH,
    @PublicAPI(usage = ACCESS)
    MEDIUM,
    @PublicAPI(usage = ACCESS)
    LOW;

    @PublicAPI(usage = ACCESS)
    public String asString() {
        return name();
    }
}
