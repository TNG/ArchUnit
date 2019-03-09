package com.tngtech.archunit.testutil.syntax.callchainexamples.longunboundedpropagation;

public class FourStepsLongUnboundImplementationStep1 implements FourStepsLongUnboundInterfaceStep1<FourStepsLongUnboundImplementationStep4> {
    @Override
    public FourStepsLongUnboundImplementationStep2 firstResult() {
        return new FourStepsLongUnboundImplementationStep2();
    }
}
