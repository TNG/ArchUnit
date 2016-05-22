package com.tngtech.archunit.core.testexamples.hierarchicalmethodcall;

public class SubClassWithCalledMethod extends SuperClassWithCalledMethod {
    public static final String maskedMethod = "maskedMethod";

    @Override
    int maskedMethod() {
        return 0;
    }
}
