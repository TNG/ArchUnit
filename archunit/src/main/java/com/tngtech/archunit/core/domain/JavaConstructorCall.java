package com.tngtech.archunit.core.domain;

import com.tngtech.archunit.core.domain.AccessTarget.ConstructorCallTarget;
import com.tngtech.archunit.core.importer.DomainBuilders.JavaConstructorCallBuilder;

public class JavaConstructorCall extends JavaCall<ConstructorCallTarget> {
    JavaConstructorCall(JavaConstructorCallBuilder builder) {
        super(builder);
    }

    @Override
    protected String descriptionTemplate() {
        return "Method <%s> calls constructor <%s>";
    }
}
