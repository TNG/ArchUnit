package com.tngtech.archunit.core;

import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;

public class JavaMethodCall extends JavaCall<MethodCallTarget> {
    JavaMethodCall(AccessRecord<MethodCallTarget> methodAccessRecord) {
        super(methodAccessRecord);
    }

    @Override
    protected String descriptionTemplate() {
        return "Method <%s> calls method <%s>";
    }
}
