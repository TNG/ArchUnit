package com.tngtech.archunit.core;

public abstract class JavaCall<T extends JavaCodeUnit<?, ?>> extends JavaAccess<T> {
    JavaCall(AccessRecord<T> accessRecord) {
        super(accessRecord);
    }

    public static final Function<JavaCall<?>, JavaCodeUnit<?, ?>> TO_TARGET = new Function<JavaCall<?>, JavaCodeUnit<?, ?>>() {
        @Override
        public JavaCodeUnit<?, ?> apply(JavaCall<?> input) {
            return input.getTarget();
        }
    };
}
