package com.tngtech.archunit.library.metrics.testobjects.componentdependency.graph.fullyabstract;

@SuppressWarnings("unused")
public abstract class SomeAbstractClass {
    static class ShouldBeIgnoredSinceNonPublic1 {
    }

    static abstract class ShouldBeIgnoredSinceNonPublic2 {
    }
}
