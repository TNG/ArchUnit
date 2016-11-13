package com.tngtech.archunit.core;

import com.tngtech.archunit.core.AccessTarget.ConstructorCallTarget;

public class JavaConstructorCall extends JavaCall<ConstructorCallTarget> {
    JavaConstructorCall(AccessRecord<ConstructorCallTarget> record) {
        super(record);
    }

    @Override
    protected String descriptionTemplate() {
        return "Method <%s> calls constructor <%s>";
    }
}
