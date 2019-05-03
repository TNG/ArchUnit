package com.tngtech.archunit.library.freeze;

class StoreUpdateFailedException extends RuntimeException {
    StoreUpdateFailedException(Throwable cause) {
        super(cause);
    }
}
