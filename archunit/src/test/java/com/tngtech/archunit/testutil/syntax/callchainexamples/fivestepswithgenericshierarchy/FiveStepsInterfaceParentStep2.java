package com.tngtech.archunit.testutil.syntax.callchainexamples.fivestepswithgenericshierarchy;

public interface FiveStepsInterfaceParentStep2<A extends FiveStepsInterfaceParentStep3<?>> {
    A secondResult();
}
