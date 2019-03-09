package com.tngtech.archunit.testutil.syntax.callchainexamples.fourstepswithgenerics;

public class FourStepsImplementationStep1 implements FourStepsInterfaceStep1 {
    @Override
    public FourStepsImplementationStep2 firstResult() {
        return new FourStepsImplementationStep2();
    }
}
