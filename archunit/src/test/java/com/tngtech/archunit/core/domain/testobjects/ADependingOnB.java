package com.tngtech.archunit.core.domain.testobjects;

public class ADependingOnB extends SuperA implements InterfaceForA {
    public ADependingOnB() {
        B b = new B();
        b.field = "new";
        b.call();
    }
}
