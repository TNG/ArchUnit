package com.tngtech.archunit.testutil.syntax.callchainexamples.threestepswithgenerics;

public class ThreeStepsImplementationStep2 implements ThreeStepsInterfaceStep2<ThreeStepsImplementationStep3> {
    @Override
    public ThreeStepsImplementationStep3 secondResult() {
        return new ThreeStepsImplementationStep3();
    }
}
