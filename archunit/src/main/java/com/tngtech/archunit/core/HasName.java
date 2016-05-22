package com.tngtech.archunit.core;

public interface HasName {
    String getName();

    interface AndFullName extends HasName {
        String getFullName();
    }
}
