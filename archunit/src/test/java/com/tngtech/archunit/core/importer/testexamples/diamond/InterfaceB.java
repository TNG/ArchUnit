package com.tngtech.archunit.core.importer.testexamples.diamond;

public interface InterfaceB extends InterfaceA {
    String implementMe = "implementMe";

    void implementMe();
}
