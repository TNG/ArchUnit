package com.tngtech.archunit.core.domain.testobjects;

@SuppressWarnings("unused")
public class ArrayComponentTypeDependencies {
    private ComponentTypeDependency[] asField;

    public ArrayComponentTypeDependencies(ComponentTypeDependency[] asConstructorParameter) {
    }

    public void asMethodParameter(ComponentTypeDependency[] asMethodParameter) {
    }

    public ComponentTypeDependency[] asReturnType() {
        return null;
    }

    private void asCallTarget() {
        new ComponentTypeDependency[0].clone();
    }
}
