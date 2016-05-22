package com.tngtech.archunit.core.testexamples.hierarchicalmethodcall;

public class SuperClassWithCalledMethod {
    public static final String method = "method";

    String method() {
        return null;
    }

    int maskedMethod() {
        return 0;
    }
}
