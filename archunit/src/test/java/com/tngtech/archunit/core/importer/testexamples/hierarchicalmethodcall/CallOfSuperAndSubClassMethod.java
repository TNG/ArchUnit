package com.tngtech.archunit.core.importer.testexamples.hierarchicalmethodcall;

public class CallOfSuperAndSubClassMethod {
    public static final String callSuperClassMethod = "callSuperClassMethod";
    public static final int callSuperClassLineNumber = 10;
    public static final String callSubClassMethod = "callSubClassMethod";
    public static final int callSubClassLineNumber = 14;

    String callSuperClassMethod() {
        return new SubClassWithCalledMethod().method();
    }

    int callSubClassMethod() {
        return new SubClassWithCalledMethod().maskedMethod();
    }
}
