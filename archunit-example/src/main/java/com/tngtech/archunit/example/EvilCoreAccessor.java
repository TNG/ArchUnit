package com.tngtech.archunit.example;

import com.tngtech.archunit.example.core.VeryCentralCore;

@SuppressWarnings("unused")
public class EvilCoreAccessor {
    void iShouldNotAccessCore() {
        new VeryCentralCore().doCoreStuff();
    }
}
