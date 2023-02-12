package com.tngtech.archunit.testutil.syntax;

import com.google.common.reflect.TypeToken;

abstract class SingleParameterProvider {
    protected final Class<?> supportedType;

    SingleParameterProvider(Class<?> supportedType) {
        this.supportedType = supportedType;
    }

    boolean canHandle(Class<?> type) {
        return supportedType.isAssignableFrom(type);
    }

    abstract Parameter get(String methodName, TypeToken<?> type);
}
