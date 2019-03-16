package com.tngtech.archunit.testutil.syntax.callchainexamples.fivestepswithgenericshierarchy;

public class FiveStepsHierarchyImplementationStep1 implements FiveStepsInterfaceParentStep1, FiveStepsInterfaceChildStep1 {
    @Override
    public FiveStepsHierarchyImplementationStep2 firstResult() {
        return new FiveStepsHierarchyImplementationStep2();
    }
}
