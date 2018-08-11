package com.tngtech.archunit.core.domain.testobjects;

public class AhavingMembersOfTypeB {
    private B b;

    public AhavingMembersOfTypeB(B b) {
        this.b = b;
    }

    B methodReturningB() {
        return null;
    }

    void methodWithParameterTypeB(String some, B b) {
    }
}
