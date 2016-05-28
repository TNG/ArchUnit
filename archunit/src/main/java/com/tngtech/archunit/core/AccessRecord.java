package com.tngtech.archunit.core;

interface AccessRecord<TARGET> {
    JavaCodeUnit<?, ?> getCaller();

    TARGET getTarget();

    int getLineNumber();

    interface FieldAccessRecord extends AccessRecord<JavaField> {
        JavaFieldAccess.AccessType getAccessType();
    }
}
