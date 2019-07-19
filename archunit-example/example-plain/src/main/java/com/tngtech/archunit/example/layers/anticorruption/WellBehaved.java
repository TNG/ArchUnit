package com.tngtech.archunit.example.layers.anticorruption;

public interface WellBehaved {
    WrappedResult someMethod();

    WrappedResult otherMethod(int param);
}
