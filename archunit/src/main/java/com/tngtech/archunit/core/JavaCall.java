package com.tngtech.archunit.core;

import com.tngtech.archunit.base.ChainableFunction;
import com.tngtech.archunit.core.AccessTarget.CodeUnitCallTarget;

public abstract class JavaCall<T extends CodeUnitCallTarget> extends JavaAccess<T> {
    JavaCall(AccessRecord<T> accessRecord) {
        super(accessRecord);
    }

    public static final ChainableFunction<JavaCall<?>, CodeUnitCallTarget> GET_TARGET =
            new ChainableFunction<JavaCall<?>, CodeUnitCallTarget>() {
                @Override
                public CodeUnitCallTarget apply(JavaCall<?> input) {
                    return input.getTarget();
                }
            };
}
