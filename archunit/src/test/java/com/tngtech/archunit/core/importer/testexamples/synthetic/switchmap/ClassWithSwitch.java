package com.tngtech.archunit.core.importer.testexamples.synthetic.switchmap;

@SuppressWarnings("unused")
public class ClassWithSwitch {
    void call(EnumToBeSwitched enumToBeSwitched) {
        switch (enumToBeSwitched) {
            case A:
                break;
            case B:
                break;
        }
    }
}
