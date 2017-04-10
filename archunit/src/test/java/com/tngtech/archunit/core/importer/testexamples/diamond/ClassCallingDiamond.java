package com.tngtech.archunit.core.importer.testexamples.diamond;

public class ClassCallingDiamond {
    public static final String callInterface = "callInterface";
    public static final String callImplementation = "callImplementation";
    public static final int callInterfaceLineNumber = 13;
    public static final int callImplementationLineNumber = 17;

    private InterfaceD interfaceD;
    private ClassImplementingD classImplementingD;

    void callInterface() {
        interfaceD.implementMe();
    }

    void callImplementation() {
        classImplementingD.implementMe();
    }
}
