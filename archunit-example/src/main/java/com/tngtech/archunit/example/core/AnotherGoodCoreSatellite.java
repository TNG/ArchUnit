package com.tngtech.archunit.example.core;

@SuppressWarnings("unused")
public class AnotherGoodCoreSatellite implements CoreSatellite {
    VeryCentralCore centralCore;

    void iAlsoMayAccessCore() {
        centralCore.doCoreStuff();
    }
}
