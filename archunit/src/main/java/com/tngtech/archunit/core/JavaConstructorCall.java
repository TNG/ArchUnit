package com.tngtech.archunit.core;

import com.tngtech.archunit.core.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaConstructorCallBuilder;

public class JavaConstructorCall extends JavaCall<ConstructorCallTarget> {
    public JavaConstructorCall(JavaConstructorCallBuilder builder) {
        super(builder);
    }

    @Override
    protected String descriptionTemplate() {
        return "Method <%s> calls constructor <%s>";
    }
}
