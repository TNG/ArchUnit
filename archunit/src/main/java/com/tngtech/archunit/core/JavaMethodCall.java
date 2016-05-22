package com.tngtech.archunit.core;

public class JavaMethodCall extends JavaMethodLikeCall<JavaMethod> {
    JavaMethodCall(AccessRecord<JavaMethod> methodAccessRecord) {
        super(methodAccessRecord);
    }

    @Override
    protected String descriptionTemplate() {
        return "Method <%s> calls method <%s>";
    }
}
