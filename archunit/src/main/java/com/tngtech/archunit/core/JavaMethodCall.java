package com.tngtech.archunit.core;

import com.tngtech.archunit.core.AccessTarget.MethodCallTarget;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaMethodCallBuilder;

public class JavaMethodCall extends JavaCall<MethodCallTarget> {
    public JavaMethodCall(JavaMethodCallBuilder builder) {
        super(builder);
    }

    @Override
    protected String descriptionTemplate() {
        return "Method <%s> calls method <%s>";
    }
}
