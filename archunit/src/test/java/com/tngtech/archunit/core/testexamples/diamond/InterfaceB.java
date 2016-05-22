package com.tngtech.archunit.core.testexamples.diamond;

public interface InterfaceB extends InterfaceA {
    String implementMe = "implementMe";

    void implementMe();
}
