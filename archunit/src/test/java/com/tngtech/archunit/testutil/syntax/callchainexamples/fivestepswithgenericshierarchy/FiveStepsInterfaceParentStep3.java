package com.tngtech.archunit.testutil.syntax.callchainexamples.fivestepswithgenericshierarchy;

public interface FiveStepsInterfaceParentStep3<B extends FiveStepsInterfaceParentStep5> {
    FiveStepsInterfaceParentStep4<B> thirdResult();
}
