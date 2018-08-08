package com.tngtech.archunit.library.diagramtests.simpledependency.origin;

import com.tngtech.archunit.library.diagramtests.simpledependency.target.SomeTargetClass;

public class SomeOriginClass {
    void accessTarget() {
        new SomeTargetClass();
    }
}
