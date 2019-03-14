package com.tngtech.archunit.core.importer.testexamples.synthetic.generics;

public class ConcreteSub extends GenericBase<Integer> {
    // Causes the compiler to create a bridge method 'void take(Object thing)'
    @Override
    void take(Integer thing) {
        super.take(thing);
    }

    void other() {
        take(1);
    }
}
