package com.tngtech.archunit.core.domain.testobjects;

public class AAccessingB {
    public AAccessingB() {
        B b = new B();
        b.field = "new";
        b.call();
    }
}
