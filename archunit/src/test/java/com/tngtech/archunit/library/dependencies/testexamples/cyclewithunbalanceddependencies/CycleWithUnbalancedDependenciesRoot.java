package com.tngtech.archunit.library.dependencies.testexamples.cyclewithunbalanceddependencies;

/**
 * Marker class to find root package of slices resulting in a graph with unbalanced dependencies,
 * i.e. some edge has only one dependency, some edge many dependencies, so we can test that
 * dependencies are omitted if there are too many.
 */
public interface CycleWithUnbalancedDependenciesRoot {
}
