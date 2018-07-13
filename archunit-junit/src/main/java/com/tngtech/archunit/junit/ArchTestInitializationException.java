package com.tngtech.archunit.junit;

import com.tngtech.archunit.Internal;

@Internal
public class ArchTestInitializationException extends RuntimeException {
    private ArchTestInitializationException(String message, Object... args) {
        super(String.format(message, args));
    }

    ArchTestInitializationException(Throwable cause, String message, Object... args) {
        super(String.format(message, args), cause);
    }

    public static void check(boolean condition, String message, Object... args) {
        if (!condition) {
            throw new ArchTestInitializationException(message, args);
        }
    }
}
