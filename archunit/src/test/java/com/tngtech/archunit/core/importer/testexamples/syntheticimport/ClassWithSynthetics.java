package com.tngtech.archunit.core.importer.testexamples.syntheticimport;

import java.util.Comparator;

@SuppressWarnings({"unused", "InnerClassMayBeStatic"})
public class ClassWithSynthetics implements Comparator<String> {
    // for (non-static) inner classes the compiler must create a synthetic field, holding a reference to the outer class
    public class ClassWithSyntheticField {
    }

    abstract class Parent {
        abstract Object overrideCovariantly();
    }

    // for covariantly overridden return types the compiler generates a bridge method which will always also be synthetic
    // i.e. the compiler overrides `Object overrideCovariantly()` and delegates to `String overrideCovariantly()`
    public class ClassWithSyntheticMethod extends Parent {
        private String nestedField;

        @Override
        String overrideCovariantly() {
            return null;
        }
    }

    // to cover type erasure, the compiler must add a bridge method with signature compare(Object, Object) here
    @Override
    public int compare(String o1, String o2) {
        return 0;
    }
}
