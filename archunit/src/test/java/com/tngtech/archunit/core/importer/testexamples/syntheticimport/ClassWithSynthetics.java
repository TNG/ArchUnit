package com.tngtech.archunit.core.importer.testexamples.syntheticimport;

import java.util.Comparator;

public class ClassWithSynthetics implements Comparator<String> {
    public class ClassWithSyntheticField {
    }

    public class ClassWithSyntheticMethod {
        private String nestedField;
    }

    public String getNestedField() {
        return new ClassWithSyntheticMethod().nestedField;
    }

    @Override
    public int compare(String o1, String o2) {
        return 0;
    }
}
