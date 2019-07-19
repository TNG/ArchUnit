package com.tngtech.archunit.example.layers;

import com.tngtech.archunit.example.layers.core.VeryCentralCore;

@SuppressWarnings("unused")
public class EvilCoreAccessor {
    void iShouldNotAccessCore() {
        new VeryCentralCore().doCoreStuff();
    }
}
