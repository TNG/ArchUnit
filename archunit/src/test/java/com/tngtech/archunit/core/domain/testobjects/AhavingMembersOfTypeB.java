package com.tngtech.archunit.core.domain.testobjects;

public class AhavingMembersOfTypeB {
    private B b;

    public AhavingMembersOfTypeB(B b) throws B.BException {
        this.b = b;
    }

    B methodReturningB() {
        return null;
    }

    void methodWithParameterTypeB(String some, B b) {
    }

    void throwingBException() throws B.BException {

    }
}
