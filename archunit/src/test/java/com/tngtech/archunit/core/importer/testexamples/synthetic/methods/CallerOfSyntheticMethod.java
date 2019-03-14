package com.tngtech.archunit.core.importer.testexamples.synthetic.methods;

public class CallerOfSyntheticMethod {
    SyntheticMethodsClass holdsSyntheticMethod;

    void call() {
        holdsSyntheticMethod.publicMethod();
    }
}
