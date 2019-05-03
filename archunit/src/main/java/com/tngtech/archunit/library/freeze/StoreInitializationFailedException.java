package com.tngtech.archunit.library.freeze;

class StoreInitializationFailedException extends RuntimeException {
    StoreInitializationFailedException(String message) {
        super(message);
    }

    StoreInitializationFailedException(Throwable cause) {
        super(cause);
    }

    StoreInitializationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
