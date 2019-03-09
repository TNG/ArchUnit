package com.tngtech.archunit.testutil.syntax.callchainexamples.fivestepswithgenericshierarchy;

public class FiveStepsHierarchyImplementationStep2
        implements FiveStepsInterfaceParentStep2<FiveStepsHierarchyImplementationStep3>,
        FiveStepsInterfaceChildStep2<FiveStepsHierarchyImplementationStep3> {

    @Override
    public FiveStepsHierarchyImplementationStep3 secondResult() {
        return new FiveStepsHierarchyImplementationStep3();
    }
}
