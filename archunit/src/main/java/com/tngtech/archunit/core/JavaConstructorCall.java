package com.tngtech.archunit.core;

public class JavaConstructorCall extends JavaMethodLikeCall<JavaConstructor> {
    JavaConstructorCall(AccessRecord<JavaConstructor> record) {
        super(record);
    }

    @Override
    protected String descriptionTemplate() {
        return "Method <%s> calls constructor <%s>";
    }
}
