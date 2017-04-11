package com.tngtech.archunit.base;

import java.net.URI;

import com.tngtech.archunit.Internal;

@Internal
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

    @Internal
    public static class LocationException extends ArchUnitException {
        public LocationException(Exception e) {
            super(e);
        }
    }

    @Internal
    public static class ReflectionException extends ArchUnitException {
        public ReflectionException(Throwable cause) {
            super(cause);
        }
    }

    @Internal
    public static class UnsupportedUriSchemeException extends ArchUnitException {
        public UnsupportedUriSchemeException(URI uri) {
            super("The scheme of the following URI is not (yet) supported: " + uri);
        }
    }

    @Internal
    public static class InconsistentClassPathException extends ArchUnitException {
        public InconsistentClassPathException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
