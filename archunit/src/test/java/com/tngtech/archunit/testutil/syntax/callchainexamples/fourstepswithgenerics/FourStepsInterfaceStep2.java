package com.tngtech.archunit.testutil.syntax.callchainexamples.fourstepswithgenerics;

public interface FourStepsInterfaceStep2<T extends FourStepsInterfaceStep4> {
    FourStepsInterfaceStep3<T> secondResult();
}
