package com.tngtech.archunit.core;

public abstract class JavaMethodLikeCall<T extends JavaMethodLike<?, ?>> extends JavaAccess<T> {
    JavaMethodLikeCall(AccessRecord<T> methodAccessRecord) {
        super(methodAccessRecord);
    }
}
