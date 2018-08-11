package com.tngtech.archunit.junit;

import com.tngtech.archunit.base.Function;

class ArchTestInitializationException extends RuntimeException {
    private ArchTestInitializationException(String message, Object... args) {
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

    static final Function<Throwable, ArchTestInitializationException> WRAP_CAUSE = new Function<Throwable, ArchTestInitializationException>() {
        @Override
        public ArchTestInitializationException apply(Throwable throwable) {
            return new ArchTestInitializationException(throwable);
        }
    };
}
