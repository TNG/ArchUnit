package com.tngtech.archunit.core;

public abstract class JavaCall<T extends JavaCodeUnit<?, ?>> extends JavaAccess<T> {
    JavaCall(AccessRecord<T> accessRecord) {
        super(accessRecord);
    }
}
