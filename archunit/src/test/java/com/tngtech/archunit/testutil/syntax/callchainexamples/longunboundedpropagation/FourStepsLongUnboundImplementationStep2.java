package com.tngtech.archunit.testutil.syntax.callchainexamples.longunboundedpropagation;

public class FourStepsLongUnboundImplementationStep2 implements FourStepsLongUnboundInterfaceStep2<FourStepsLongUnboundImplementationStep4> {
    @Override
    public FourStepsLongUnboundImplementationStep3 secondResult() {
        return new FourStepsLongUnboundImplementationStep3();
    }
}
