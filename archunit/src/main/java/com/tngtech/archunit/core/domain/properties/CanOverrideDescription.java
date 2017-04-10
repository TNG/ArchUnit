package com.tngtech.archunit.core.domain.properties;

public interface CanOverrideDescription<SELF> {
    SELF as(String newDescription);
}
