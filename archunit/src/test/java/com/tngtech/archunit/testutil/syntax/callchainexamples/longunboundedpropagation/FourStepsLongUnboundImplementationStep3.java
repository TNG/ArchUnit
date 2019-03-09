package com.tngtech.archunit.testutil.syntax.callchainexamples.longunboundedpropagation;

public class FourStepsLongUnboundImplementationStep3 implements FourStepsLongUnboundInterfaceStep3<FourStepsLongUnboundImplementationStep4> {
    @Override
    public FourStepsLongUnboundImplementationStep4 thirdResult() {
        return new FourStepsLongUnboundImplementationStep4();
    }
}
