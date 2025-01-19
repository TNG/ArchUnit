package com.tngtech.archunit.junit.internal;

class ArchTestInitializationException extends RuntimeException {
    ArchTestInitializationException(String message, Object... args) {
        super(String.format(message, args));
    }

    ArchTestInitializationException(Throwable cause) {
        super(cause);
    }

    ArchTestInitializationException(Throwable cause, String message, Object... args) {
        super(String.format(message, args), cause);
    }

    static void check(boolean condition, String message, Object... args) {
        if (!condition) {
            throw new ArchTestInitializationException(message, args);
        }
    }
}
