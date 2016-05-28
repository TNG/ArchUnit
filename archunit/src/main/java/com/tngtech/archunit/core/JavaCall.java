package com.tngtech.archunit.core;

public abstract class JavaCall<T extends JavaMethodLike<?, ?>> extends JavaAccess<T> {
    JavaCall(AccessRecord<T> methodAccessRecord) {
        super(methodAccessRecord);
    }
}
