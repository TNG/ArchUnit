package com.tngtech.archunit.core.domain.properties;

import com.tngtech.archunit.PublicAPI;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public interface HasDescription {
    @PublicAPI(usage = ACCESS)
    String getDescription();
}
