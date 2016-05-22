package com.tngtech.archunit.core;

import com.google.common.base.Optional;

public class Optionals {
    public static <T> T valueOrException(Optional<T> optional, RuntimeException e) {
        if (optional.isPresent()) {
            return optional.get();
        }
        throw e;
    }
}
