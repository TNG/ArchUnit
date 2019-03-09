package com.tngtech.archunit.testutil.syntax.callchainexamples.fourstepswithgenerics;

public class FourStepsImplementationStep2 implements FourStepsInterfaceStep2<FourStepsImplementationStep4> {
    @Override
    public FourStepsImplementationStep3 secondResult() {
        return new FourStepsImplementationStep3();
    }
}
