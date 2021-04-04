package com.tngtech.archunit.library.metrics.testobjects.componentdependency.graph.fullyabstract;

import com.tngtech.archunit.library.metrics.testobjects.componentdependency.graph.fullyconcrete.SomeConcreteClass;

@SuppressWarnings("unused")
public abstract class SomeOtherAbstractClass {
    SomeConcreteClass dependency;
}
