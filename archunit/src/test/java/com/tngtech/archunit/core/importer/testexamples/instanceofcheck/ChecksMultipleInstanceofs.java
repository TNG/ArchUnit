package com.tngtech.archunit.core.importer.testexamples.instanceofcheck;

@SuppressWarnings({"StatementWithEmptyBody", "unused", "ConstantConditions"})
public class ChecksMultipleInstanceofs {
    static {
        boolean foo = ((Object) null) instanceof InstanceofChecked;
    }

    public ChecksMultipleInstanceofs(Object param) {
        if (param instanceof InstanceofChecked) {
        }
    }

    boolean method(Object param) {
        return param instanceof InstanceofChecked;
    }
}
