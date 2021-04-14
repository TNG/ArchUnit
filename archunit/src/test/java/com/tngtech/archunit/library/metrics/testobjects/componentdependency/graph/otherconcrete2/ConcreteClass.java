package com.tngtech.archunit.library.metrics.testobjects.componentdependency.graph.otherconcrete2;

import com.tngtech.archunit.library.metrics.testobjects.componentdependency.graph.fullyabstract.SomeAbstractClass;
import com.tngtech.archunit.library.metrics.testobjects.componentdependency.graph.fullyconcrete.SomeConcreteClass;

public class ConcreteClass {
    SomeAbstractClass dependency2;
    SomeConcreteClass dependency;
}
