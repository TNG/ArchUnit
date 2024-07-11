package com.tngtech.archunit.core.importer.testexamples.referencedclassobjects;

import java.io.FilterInputStream;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class ReferencingClassObjectsFromLocalVariable {

    static {
        FilterInputStream streamStatic = null;
        List<Double> listStatic = new ArrayList<>();
        listStatic.size(); // if listStatic is not used it is optimized away. Surprisingly this is not the case for streamStatic above
    }

    void reference() { FilterInputStream stream = null; }

    void referenceByGeneric() { List<FilterInputStream> list = null; }
}
