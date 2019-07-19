package com.tngtech.archunit.example.layers.core;

@SuppressWarnings("unused")
public class GoodCoreSatellite implements CoreSatellite {
    VeryCentralCore centralCore;

    void iMayAccessCore() {
        centralCore.doCoreStuff();
    }
}
