package com.tngtech.archunit.core.importer.testexamples.instanceofcheck;

@SuppressWarnings("StatementWithEmptyBody")
public class ChecksInstanceofInConstructor {
    public ChecksInstanceofInConstructor(Object param) {
        if (param instanceof InstanceofChecked) {
        }
    }
}
