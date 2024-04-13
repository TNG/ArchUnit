package com.tngtech.archunit.core.importer.testexamples.outsideofclasspath;

import java.io.Serializable;

public class ExistingDependency {
    private Serializable serializableField;
    private String description;

    <T extends Serializable & GimmeADescription> void init(T seed) {
        serializableField = seed;
        description = seed.gimme();
    }

    interface GimmeADescription {
        String gimme();
    }
}
