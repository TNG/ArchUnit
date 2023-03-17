package com.tngtech.archunit.testutil.syntax;

import com.google.common.reflect.TypeToken;

public abstract class SingleParameterProvider {
    protected final Class<?> supportedType;

    public SingleParameterProvider(Class<?> supportedType) {
        this.supportedType = supportedType;
    }

    protected boolean canHandle(String methodName, Class<?> type) {
        return supportedType.isAssignableFrom(type);
    }

    public abstract Parameter get(String methodName, TypeToken<?> type);
}
