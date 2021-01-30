package com.tngtech.archunit.core.importer.testexamples.hierarchicalmethodcall;

public class SuperclassWithCalledMethod {
    public static final String method = "method";

    String method() {
        return null;
    }

    int maskedMethod() {
        return 0;
    }
}
