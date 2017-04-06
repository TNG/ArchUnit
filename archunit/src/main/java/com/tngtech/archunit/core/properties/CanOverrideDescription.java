package com.tngtech.archunit.core.properties;

public interface CanOverrideDescription<SELF> {
    SELF as(String newDescription);
}
