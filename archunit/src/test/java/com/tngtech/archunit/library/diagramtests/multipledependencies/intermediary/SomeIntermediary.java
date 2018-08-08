package com.tngtech.archunit.library.diagramtests.multipledependencies.intermediary;

import com.tngtech.archunit.library.diagramtests.multipledependencies.origin.SomeOrigin;
import com.tngtech.archunit.library.diagramtests.multipledependencies.target.SomeTarget;

public class SomeIntermediary {
    private SomeTarget legalFieldOfTypeSomeTarget;
    private SomeOrigin illegalFieldOfTypeOrigin;
}
