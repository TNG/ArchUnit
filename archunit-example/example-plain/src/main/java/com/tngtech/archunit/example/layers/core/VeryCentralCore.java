package com.tngtech.archunit.example.layers.core;

import com.tngtech.archunit.example.layers.SomeOtherBusinessInterface;
import com.tngtech.archunit.example.layers.web.AnnotatedController;

@HighSecurity
@SuppressWarnings("unused")
public class VeryCentralCore implements SomeOtherBusinessInterface {
    public static final String DO_CORE_STUFF_METHOD_NAME = "doCoreStuff";

    public void doCoreStuff() {
    }

    void coreDoingIllegalStuff() {
        new AnnotatedController();
    }
}
