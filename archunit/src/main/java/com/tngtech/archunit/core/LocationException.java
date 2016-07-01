package com.tngtech.archunit.core;

public class LocationException extends RuntimeException {
    public LocationException(Exception e) {
        super(e);
    }
}
