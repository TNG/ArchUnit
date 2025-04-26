package com.tngtech.archunit.testutil;

import org.junit.jupiter.params.provider.Arguments;

public class DataProviders {
    public static Arguments $(Object... arguments) {
        return Arguments.of(arguments);
    }
}
