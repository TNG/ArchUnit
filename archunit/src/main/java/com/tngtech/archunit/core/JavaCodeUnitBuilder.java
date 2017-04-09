package com.tngtech.archunit.core;

import java.util.List;

import com.google.common.collect.ImmutableList;

abstract class JavaCodeUnitBuilder<OUTPUT, SELF extends JavaCodeUnitBuilder<OUTPUT, SELF>> extends JavaMemberBuilder<OUTPUT, SELF> {
    private JavaType returnType;
    private List<JavaType> parameters;

    SELF withReturnType(JavaType type) {
        returnType = type;
        return self();
    }

    SELF withParameters(List<JavaType> parameters) {
        this.parameters = parameters;
        return self();
    }

    JavaClass getReturnType() {
        return get(returnType.getName());
    }

    public List<JavaClass> getParameters() {
        ImmutableList.Builder<JavaClass> result = ImmutableList.builder();
        for (JavaType parameter : parameters) {
            result.add(get(parameter.getName()));
        }
        return result.build();
    }
}
