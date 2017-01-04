package com.tngtech.archunit.base;

public interface Function<F, T> {
    T apply(F input);
}
