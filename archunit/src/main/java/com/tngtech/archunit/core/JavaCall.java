package com.tngtech.archunit.core;

import com.tngtech.archunit.core.AccessTarget.CodeUnitCallTarget;

public abstract class JavaCall<T extends CodeUnitCallTarget> extends JavaAccess<T> {
    JavaCall(AccessRecord<T> accessRecord) {
        super(accessRecord);
    }
}
