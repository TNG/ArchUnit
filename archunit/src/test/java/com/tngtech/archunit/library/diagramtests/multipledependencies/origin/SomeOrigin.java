package com.tngtech.archunit.library.diagramtests.multipledependencies.origin;

import com.tngtech.archunit.library.diagramtests.multipledependencies.intermediary.SomeIntermediary;
import com.tngtech.archunit.library.diagramtests.multipledependencies.target.SomeTarget;

public class SomeOrigin {
    private SomeIntermediary legalFieldOfTypeIntermediary;
    private SomeTarget illegalFieldOfTypeTarget;
}
