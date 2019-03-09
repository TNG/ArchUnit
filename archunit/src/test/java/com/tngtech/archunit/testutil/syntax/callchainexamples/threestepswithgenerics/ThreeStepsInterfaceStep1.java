package com.tngtech.archunit.testutil.syntax.callchainexamples.threestepswithgenerics;

public interface ThreeStepsInterfaceStep1<T extends ThreeStepsInterfaceStep3> {
    ThreeStepsInterfaceStep2<T> firstResult();
}
