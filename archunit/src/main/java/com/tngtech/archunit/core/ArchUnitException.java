package com.tngtech.archunit.core;

import java.net.URL;

public class ArchUnitException extends RuntimeException {
    protected ArchUnitException(String message, Throwable cause) {
        super(message, cause);
    }

    protected ArchUnitException(Throwable cause) {
        super(cause);
    }

    public static class LocationException extends ArchUnitException {
        LocationException(Exception e) {
            super(e);
        }
    }

    public static class ReflectionException extends ArchUnitException {
        public ReflectionException(Throwable cause) {
            super(cause);
        }
    }

    public static class UnidentifiableTargetException extends RuntimeException {
        UnidentifiableTargetException(String message) {
            super(message);
        }
    }

    public static class UnsupportedUrlProtocolException extends RuntimeException {
        UnsupportedUrlProtocolException(URL url) {
            super("The protocol of the following URL is not (yet) supported: " + url);
        }
    }
}
