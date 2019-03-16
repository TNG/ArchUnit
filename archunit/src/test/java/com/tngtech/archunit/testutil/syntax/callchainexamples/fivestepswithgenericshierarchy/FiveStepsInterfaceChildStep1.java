package com.tngtech.archunit.testutil.syntax.callchainexamples.fivestepswithgenericshierarchy;

public interface FiveStepsInterfaceChildStep1 extends FiveStepsInterfaceParentStep1 {
    @Override
    FiveStepsInterfaceChildStep2<?> firstResult();
}
