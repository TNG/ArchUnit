package com.tngtech.archunit.core.importer.testexamples.instanceofcheck;

@SuppressWarnings("unused")
public class ChecksInstanceofInMethod {
    boolean method(Object param) {
        return param instanceof InstanceofChecked;
    }
}
