package com.tngtech.archunit.core.importer.testexamples.hierarchicalmethodcall;

public class CallOfSuperAndSubclassMethod {
    public static final String callSuperclassMethod = "callSuperclassMethod";
    public static final int callSuperclassLineNumber = 10;
    public static final String callSubclassMethod = "callSubclassMethod";
    public static final int callSubclassLineNumber = 14;

    String callSuperclassMethod() {
        return new SubclassWithCalledMethod().method();
    }

    int callSubclassMethod() {
        return new SubclassWithCalledMethod().maskedMethod();
    }
}
