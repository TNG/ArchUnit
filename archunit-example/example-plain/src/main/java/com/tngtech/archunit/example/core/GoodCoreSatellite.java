package com.tngtech.archunit.example.core;

@SuppressWarnings("unused")
public class GoodCoreSatellite implements CoreSatellite {
    VeryCentralCore centralCore;

    void iMayAccessCore() {
        centralCore.doCoreStuff();
    }
}
