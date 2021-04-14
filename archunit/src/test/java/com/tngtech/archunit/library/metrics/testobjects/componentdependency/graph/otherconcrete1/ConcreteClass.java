package com.tngtech.archunit.library.metrics.testobjects.componentdependency.graph.otherconcrete1;

import com.tngtech.archunit.library.metrics.testobjects.componentdependency.graph.fullyabstract.SomeAbstractClass;

@SuppressWarnings("unused")
public class ConcreteClass {
    com.tngtech.archunit.library.metrics.testobjects.componentdependency.graph.otherconcrete2.ConcreteClass dependency1;
    com.tngtech.archunit.library.metrics.testobjects.componentdependency.graph.otherconcrete2.ConcreteClass redundantDependency1;
    SomeAbstractClass dependency2;
}
