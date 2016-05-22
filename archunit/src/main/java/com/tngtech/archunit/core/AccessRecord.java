package com.tngtech.archunit.core;

interface AccessRecord<TARGET> {
    JavaMethodLike<?, ?> getCaller();

    TARGET getTarget();

    int getLineNumber();

    interface FieldAccessRecord extends AccessRecord<JavaField> {
        JavaFieldAccess.AccessType getAccessType();
    }
}
