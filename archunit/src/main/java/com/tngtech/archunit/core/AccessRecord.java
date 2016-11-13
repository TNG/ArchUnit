package com.tngtech.archunit.core;

import com.tngtech.archunit.core.AccessTarget.FieldAccessTarget;

interface AccessRecord<TARGET extends AccessTarget> {
    JavaCodeUnit<?, ?> getCaller();

    TARGET getTarget();

    int getLineNumber();

    interface FieldAccessRecord extends AccessRecord<FieldAccessTarget> {
        JavaFieldAccess.AccessType getAccessType();
    }
}
