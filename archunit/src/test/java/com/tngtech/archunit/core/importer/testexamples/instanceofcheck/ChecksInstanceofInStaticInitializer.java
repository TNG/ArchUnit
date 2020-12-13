package com.tngtech.archunit.core.importer.testexamples.instanceofcheck;

@SuppressWarnings({"unused", "ConstantConditions"})
public class ChecksInstanceofInStaticInitializer {
    static {
        boolean foo = ((Object) null) instanceof InstanceofChecked;
    }
}
