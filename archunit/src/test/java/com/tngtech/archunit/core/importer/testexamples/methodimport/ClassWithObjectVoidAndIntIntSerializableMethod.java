package com.tngtech.archunit.core.importer.testexamples.methodimport;

import java.io.Serializable;

public class ClassWithObjectVoidAndIntIntSerializableMethod {
    public void consume(Object object) {
    }

    private Serializable createSerializable(int one, int two) {
        return null;
    }
}
