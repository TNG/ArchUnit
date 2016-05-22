package com.tngtech.archunit.lang.conditions.testobjects;

public class CallerClass {
    public static final String methodThatGetsPublicString = "methodThatGetsPublicString";
    public static final String[] getAccessOfPublicStringLineNumbers = new String[]{"14", "15"};
    public static final String methodThatSetsPublicString = "methodThatSetsPublicString";
    public static final String[] setAccessOfPublicStringLineNumbers = new String[]{"19"};
    public static final String methodThatCallsAppendString = "methodThatCallsAppendString";
    public static final String callOfAppendStringLineNumber = "23";

    private TargetClass target;

    String methodThatGetsPublicString() {
        System.out.println(target.publicString);
        return target.publicString;
    }

    void methodThatSetsPublicString() {
        target.publicString = "changed";
    }

    void methodThatCallsAppendString() {
        target.appendString("something");
    }

    void methodThatCallsConstructor() {
        new TargetClass();
    }
}
