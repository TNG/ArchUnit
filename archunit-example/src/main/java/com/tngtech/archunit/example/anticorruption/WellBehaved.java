package com.tngtech.archunit.example.anticorruption;

public interface WellBehaved {
    WrappedResult someMethod();

    WrappedResult otherMethod(int param);
}
