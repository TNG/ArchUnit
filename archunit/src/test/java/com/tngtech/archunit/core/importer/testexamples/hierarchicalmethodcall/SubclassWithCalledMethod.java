package com.tngtech.archunit.core.importer.testexamples.hierarchicalmethodcall;

public class SubclassWithCalledMethod extends SuperclassWithCalledMethod {
    public static final String maskedMethod = "maskedMethod";

    @Override
    int maskedMethod() {
        return 0;
    }
}
