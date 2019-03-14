package com.tngtech.archunit.core.importer.testexamples.synthetic.covariantreturn;

public class ChildReturningType extends BaseReturningType {
    @Override
    Integer returning() {
        return null;
    }
}
