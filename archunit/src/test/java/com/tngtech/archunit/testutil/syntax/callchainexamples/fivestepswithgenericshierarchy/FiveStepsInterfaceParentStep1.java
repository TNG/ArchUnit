package com.tngtech.archunit.testutil.syntax.callchainexamples.fivestepswithgenericshierarchy;

public interface FiveStepsInterfaceParentStep1 {
    FiveStepsInterfaceParentStep2<?> firstResult();
}
