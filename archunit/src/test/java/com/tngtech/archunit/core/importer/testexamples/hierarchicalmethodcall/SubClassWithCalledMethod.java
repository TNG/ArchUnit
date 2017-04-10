package com.tngtech.archunit.core.importer.testexamples.hierarchicalmethodcall;

public class SubClassWithCalledMethod extends SuperClassWithCalledMethod {
    public static final String maskedMethod = "maskedMethod";

    @Override
    int maskedMethod() {
        return 0;
    }
}
