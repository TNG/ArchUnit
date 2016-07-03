package com.tngtech.archunit.core;

public interface Function<F, T> {
    T apply(F input);
}
