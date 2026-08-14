package com.tngtech.archunit.core.importer.testexamples.referencedclassobjects;

import java.io.FilterInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unused")
public class ReferencingClassObjectsFromLocalVariable<T extends Number> {

    static {
        FilterInputStream streamStatic = null;
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection") List<Double> listStatic = new ArrayList<>();
        //noinspection ResultOfMethodCallIgnored
        listStatic.size(); // if listStatic is not used it is optimized away. Surprisingly this is not the case for streamStatic above
    }

    void reference() {
        FilterInputStream stream = null;
        InputStream stream2 = null;
        System.out.println(stream);
        System.out.println(stream2);
        // after statement and comment
        FilterInputStream stream3 = null;
        InputStream stream4 = null;
        System.out.println(stream3);
        System.out.println(stream4);
        {
            // in block
            FilterInputStream stream5 = null;
            InputStream stream6 = null;
            System.out.println(stream5);
            System.out.println(stream6);
        }
    }

    void referenceByGeneric() {
        List<FilterInputStream> list = null;
        List<InputStream> list2 = null;
        // multiple generic parameters
        Map<FilterInputStream, InputStream> map = null;
        // nested generic parameters
        Map<Set<FilterInputStream>, InputStream> map2 = null;
        System.out.println(list);
        System.out.println(list2);
        System.out.println(map);
        System.out.println(map2);
    }

    void referenceToOwnGenericType() {
        T myType = null;
        System.out.println(myType);
    }
}
