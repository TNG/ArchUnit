package com.tngtech.archunit.testutil.syntax.callchainexamples.fourstepswithgenerics;

public interface FourStepsInterfaceStep1 {
    FourStepsInterfaceStep2<?> firstResult();
}
