package com.tngtech.archunit.testutil.syntax.callchainexamples.fivestepswithgenericshierarchy;

public class FiveStepsHierarchyImplementationStep4 implements FiveStepsInterfaceChildStep4<FiveStepsHierarchyImplementationStep5> {
    @Override
    public FiveStepsHierarchyImplementationStep5 fourthResult() {
        return new FiveStepsHierarchyImplementationStep5();
    }
}
