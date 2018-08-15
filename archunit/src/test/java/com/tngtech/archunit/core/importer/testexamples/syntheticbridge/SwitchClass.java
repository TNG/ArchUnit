package com.tngtech.archunit.core.importer.testexamples.syntheticbridge;

public class SwitchClass {
    void doIt(FooEnum foo) {
        switch (foo) {
        case X:
            break;
        case Y:
            break;
        case Z:
            break;
        }
    }
}
