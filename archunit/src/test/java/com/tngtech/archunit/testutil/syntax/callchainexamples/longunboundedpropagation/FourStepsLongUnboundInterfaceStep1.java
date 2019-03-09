package com.tngtech.archunit.testutil.syntax.callchainexamples.longunboundedpropagation;

public interface FourStepsLongUnboundInterfaceStep1<T extends FourStepsLongUnboundInterfaceStep4> {
    FourStepsLongUnboundInterfaceStep2<T> firstResult();
}
