package com.tngtech.archunit.core;

import java.net.URL;

public class ArchUnitException extends RuntimeException {
    ArchUnitException(String message, Throwable cause) {
        super(message, cause);
    }

    ArchUnitException(String message) {
        super(message);
    }

    ArchUnitException(Throwable cause) {
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

    public static class UnsupportedUrlProtocolException extends ArchUnitException {
        UnsupportedUrlProtocolException(URL url) {
            super("The protocol of the following URL is not (yet) supported: " + url);
        }
    }

    public static class InconsistentClassPathException extends ArchUnitException {
        InconsistentClassPathException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
