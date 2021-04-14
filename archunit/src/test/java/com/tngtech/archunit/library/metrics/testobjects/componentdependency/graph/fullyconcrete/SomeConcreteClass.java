package com.tngtech.archunit.library.metrics.testobjects.componentdependency.graph.fullyconcrete;

@SuppressWarnings("unused")
public class SomeConcreteClass {
    static class ShouldBeIgnoredSinceNonPublic1 {
    }

    static abstract class ShouldBeIgnoredSinceNonPublic2 {
    }
}
