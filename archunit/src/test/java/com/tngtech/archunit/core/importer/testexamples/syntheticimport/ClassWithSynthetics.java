package com.tngtech.archunit.core.importer.testexamples.syntheticimport;

import java.util.Comparator;

@SuppressWarnings({"unused", "InnerClassMayBeStatic"})
public class ClassWithSynthetics implements Comparator<String> {
    // for (non-static) inner classes the compiler must create a synthetic field, holding a reference to the outer class
    public class ClassWithSyntheticField {
    }

    // for accesses to private fields of inner classes, the compiler must create a synthetic method to allow access to this field
    // thus together with the method 'getNestedField', this causes the existence of a synthetic method
    public class ClassWithSyntheticMethod {
        private String nestedField;
    }

    public String getNestedField() {
        return new ClassWithSyntheticMethod().nestedField;
    }

    // to cover type erasure, the compiler must add a bridge method with signature compare(Object, Object) here
    @Override
    public int compare(String o1, String o2) {
        return 0;
    }
}
