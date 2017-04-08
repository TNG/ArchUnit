package com.tngtech.archunit.base;

import java.net.URI;

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
        public LocationException(Exception e) {
            super(e);
        }
    }

    public static class ReflectionException extends ArchUnitException {
        public ReflectionException(Throwable cause) {
            super(cause);
        }
    }

    public static class UnsupportedUriSchemeException extends ArchUnitException {
        public UnsupportedUriSchemeException(URI uri) {
            super("The scheme of the following URI is not (yet) supported: " + uri);
        }
    }

    public static class InconsistentClassPathException extends ArchUnitException {
        public InconsistentClassPathException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
