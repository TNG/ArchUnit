package com.tngtech.archunit.library.freeze;

class StoreReadException extends RuntimeException {
    StoreReadException(Throwable cause) {
        super(cause);
    }
}
