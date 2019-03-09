package com.tngtech.archunit.testutil.syntax.callchainexamples.threestepswithgenerics;

public class ThreeStepsImplementationStep1 implements ThreeStepsInterfaceStep1<ThreeStepsImplementationStep3> {
    @Override
    public ThreeStepsImplementationStep2 firstResult() {
        return new ThreeStepsImplementationStep2();
    }
}
