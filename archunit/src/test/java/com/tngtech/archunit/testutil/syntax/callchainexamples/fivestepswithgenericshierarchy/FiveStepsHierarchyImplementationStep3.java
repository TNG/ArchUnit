package com.tngtech.archunit.testutil.syntax.callchainexamples.fivestepswithgenericshierarchy;

public class FiveStepsHierarchyImplementationStep3 implements FiveStepsInterfaceChildStep3<FiveStepsHierarchyImplementationStep5> {
    @Override
    public FiveStepsInterfaceChildStep4<FiveStepsHierarchyImplementationStep5> thirdResult() {
        return new FiveStepsHierarchyImplementationStep4();
    }
}
