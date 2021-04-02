package com.tngtech.archunit.library.metrics;

import java.util.Map;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import static com.tngtech.archunit.library.metrics.TestElement.GET_DEPENDENCIES;
import static com.tngtech.archunit.library.metrics.TestMetricsComponentDependencyGraph.fromNode;
import static com.tngtech.archunit.library.metrics.TestMetricsComponentDependencyGraph.graph;
import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;

public class MetricsComponentDependencyGraphTest {

    @Test
    public void should_create_simple_dependency_between_two_components() {
        TestElement elementOfComponent1 = new TestElement();
        TestElement elementOfComponent2 = new TestElement();

        elementOfComponent1.addDependency(elementOfComponent2);

        MetricsComponent<TestElement> component1 = MetricsComponent.of("component1", elementOfComponent1);
        MetricsComponent<TestElement> component2 = MetricsComponent.of("component2", elementOfComponent2);

        MetricsComponentDependencyGraph<TestElement> graph = MetricsComponentDependencyGraph.of(ImmutableSet.of(component1, component2), GET_DEPENDENCIES);

        assertThat(graph.getDirectDependenciesOf(component1)).containsOnly(component2);
    }

    @Test
    public void should_ignore_dependencies_inside_of_same_component() {
        TestElement component1Element1 = new TestElement();
        TestElement component1Element2 = new TestElement();
        TestElement component2Element1 = new TestElement();

        component1Element1.addDependency(component1Element2);
        component1Element1.addDependency(component2Element1);

        MetricsComponent<TestElement> component1 = MetricsComponent.of("component1", component1Element1, component1Element2);
        MetricsComponent<TestElement> component2 = MetricsComponent.of("component2", component2Element1);

        MetricsComponentDependencyGraph<TestElement> graph = MetricsComponentDependencyGraph.of(ImmutableSet.of(component1, component2), GET_DEPENDENCIES);

        assertThat(graph.getDirectDependenciesOf(component1)).containsOnly(component2);
    }

    @Test
    public void should_ignore_dependencies_outside_of_components() {
        TestElement elementInsideOfComponent = new TestElement();
        TestElement elementOutsideOfComponent = new TestElement();

        elementInsideOfComponent.addDependency(elementOutsideOfComponent);

        MetricsComponent<TestElement> component = MetricsComponent.of("component", elementInsideOfComponent);
        MetricsComponentDependencyGraph<TestElement> graph = MetricsComponentDependencyGraph.of(singleton(component), GET_DEPENDENCIES);

        assertThat(graph.getDirectDependenciesOf(component)).as("dependencies of component").isEmpty();
    }

    @Test
    public void findsTransitiveDependenciesInAcyclicGraph() {
        Map<String, MetricsComponent<TestElement>> testComponents = graph(
                fromNode("A").toNodes("B", "C")
                        .fromNode("C").toNodes("D")
        ).toComponentsByIdentifier();
        MetricsComponent<TestElement> a = testComponents.get("A");
        MetricsComponent<TestElement> b = testComponents.get("B");
        MetricsComponent<TestElement> c = testComponents.get("C");
        MetricsComponent<TestElement> d = testComponents.get("D");

        MetricsComponentDependencyGraph<TestElement> graph = MetricsComponentDependencyGraph.of(ImmutableSet.of(a, b, c, d), GET_DEPENDENCIES);

        assertThat(graph.getTransitiveDependenciesOf(a)).containsOnly(b, c, d);
        assertThat(graph.getTransitiveDependenciesOf(b)).isEmpty();
        assertThat(graph.getTransitiveDependenciesOf(c)).containsOnly(d);
        assertThat(graph.getTransitiveDependenciesOf(d)).isEmpty();
    }

    @Test
    public void findsTransitiveDependenciesInCyclicGraph() {
        Map<String, MetricsComponent<TestElement>> testComponents = graph(
                fromNode("A").toNodes("B", "C", "D")
                        .fromNode("C").toNodes("A")
                        .fromNode("D").toNodes("E")
                        .fromNode("E").toNodes("A")
        ).toComponentsByIdentifier();
        MetricsComponent<TestElement> a = testComponents.get("A");
        MetricsComponent<TestElement> b = testComponents.get("B");
        MetricsComponent<TestElement> c = testComponents.get("C");
        MetricsComponent<TestElement> d = testComponents.get("D");
        MetricsComponent<TestElement> e = testComponents.get("E");

        MetricsComponentDependencyGraph<TestElement> graph = MetricsComponentDependencyGraph.of(ImmutableSet.of(a, b, c, d, e), GET_DEPENDENCIES);

        assertThat(graph.getTransitiveDependenciesOf(a)).containsOnly(b, c, d, e, a);
        assertThat(graph.getTransitiveDependenciesOf(b)).isEmpty();
        assertThat(graph.getTransitiveDependenciesOf(c)).containsOnly(a, b, c, d, e);
        assertThat(graph.getTransitiveDependenciesOf(d)).containsOnly(e, a, b, c, d);
        assertThat(graph.getTransitiveDependenciesOf(e)).containsOnly(e, a, b, c, d);
    }
}
