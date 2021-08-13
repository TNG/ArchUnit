package com.tngtech.archunit.core.importer.testexamples.codeunitreferences;

import java.util.function.Consumer;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class Origin {
    void referencesConstructor() {
        Supplier<Target> a = Target::new;
    }

    void referencesMethod() {
        Consumer<Target> b = Target::call;
    }
}
