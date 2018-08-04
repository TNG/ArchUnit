package com.tngtech.archunit.example.core;

import com.tngtech.archunit.example.SomeOtherBusinessInterface;
import com.tngtech.archunit.example.web.AnnotatedController;

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
