package com.tngtech.archunit.testutil.syntax.callchainexamples.fourstepswithgenerics;

public class FourStepsImplementationStep3 implements FourStepsInterfaceStep3<FourStepsImplementationStep4> {
    @Override
    public FourStepsImplementationStep4 thirdResult() {
        return new FourStepsImplementationStep4();
    }
}
