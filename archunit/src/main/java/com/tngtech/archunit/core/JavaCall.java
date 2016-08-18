package com.tngtech.archunit.core;

public abstract class JavaCall<T extends JavaCodeUnit<?, ?>> extends JavaAccess<T> {
    JavaCall(AccessRecord<T> accessRecord) {
        super(accessRecord);
    }

    public static final ChainableFunction<JavaCall<?>, JavaCodeUnit<?, ?>> GET_TARGET =
            new ChainableFunction<JavaCall<?>, JavaCodeUnit<?, ?>>() {
                @Override
                public JavaCodeUnit<?, ?> apply(JavaCall<?> input) {
                    return input.getTarget();
                }
            };
}
