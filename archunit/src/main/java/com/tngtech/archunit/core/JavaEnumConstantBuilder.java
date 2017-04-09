package com.tngtech.archunit.core;

public class JavaEnumConstantBuilder {
    private JavaClass declaringClass;
    private String name;

    public JavaEnumConstantBuilder withDeclaringClass(final JavaClass declaringClass) {
        this.declaringClass = declaringClass;
        return this;
    }

    public JavaEnumConstantBuilder withName(final String name) {
        this.name = name;
        return this;
    }

    public JavaClass getDeclaringClass() {
        return declaringClass;
    }

    public String getName() {
        return name;
    }

    public JavaEnumConstant build() {
        return new JavaEnumConstant(this);
    }
}
