package com.tngtech.archunit.testutil.syntax.callchainexamples.fivestepswithgenericshierarchy;

public interface FiveStepsInterfaceChildStep2<A extends FiveStepsInterfaceChildStep3<?>> extends FiveStepsInterfaceParentStep2<A> {
    @Override
    A secondResult();
}
