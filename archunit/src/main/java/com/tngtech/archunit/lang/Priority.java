package com.tngtech.archunit.lang;

public enum Priority {
    HIGH, MEDIUM, LOW;

    public String asString() {
        return name();
    }
}
