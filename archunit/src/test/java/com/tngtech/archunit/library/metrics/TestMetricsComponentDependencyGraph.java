package com.tngtech.archunit.library.metrics;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Maps;
import com.google.common.collect.Ordering;

import static com.google.common.base.Functions.toStringFunction;
import static java.lang.System.lineSeparator;

public class TestMetricsComponentDependencyGraph {
    private final Set<TestDependency> dependencies;
    private final MetricsComponents<TestElement> components;

    private TestMetricsComponentDependencyGraph(DependenciesBuilder dependenciesBuilder) {
        Map<String, TestElement> elements = new HashMap<>();
        dependencies = dependenciesBuilder.dependencies;
        for (TestDependency dependency : dependencies) {
            getElement(elements, dependency.origin).addDependency(getElement(elements, dependency.target));
        }
        Set<MetricsComponent<TestElement>> components = new HashSet<>();
        for (Map.Entry<String, TestElement> nameToElement : elements.entrySet()) {
            components.add(MetricsComponent.of(nameToElement.getKey(), nameToElement.getValue()));
        }
        this.components = MetricsComponents.of(components);
    }

    public MetricsComponents<TestElement> toComponents() {
        return components;
    }

    public Map<String, MetricsComponent<TestElement>> toComponentsByIdentifier() {
        return Maps.uniqueIndex(components, new Function<MetricsComponent<TestElement>, String>() {
            @Override
            public String apply(MetricsComponent<TestElement> input) {
                return input.getIdentifier();
            }
        });
    }

    @Override
    public String toString() {
        List<String> formattedDependencies = FluentIterable.from(dependencies).transform(toStringFunction()).toSortedList(Ordering.<String>natural());
        return String.format("graph {%n%s%n}", Joiner.on(lineSeparator()).join(formattedDependencies));
    }

    private static TestElement getElement(Map<String, TestElement> elements, String name) {
        if (!elements.containsKey(name)) {
            elements.put(name, new TestElement());
        }
        return elements.get(name);
    }

    public static TestMetricsComponentDependencyGraph graph(DependenciesBuilder dependenciesBuilder) {
        return new TestMetricsComponentDependencyGraph(dependenciesBuilder);
    }

    public static DependenciesBuilder fromNode(String name) {
        return new DependenciesBuilder(name);
    }

    public static class DependenciesBuilder {
        private final Set<TestDependency> dependencies = new HashSet<>();

        private String currentOrigin;

        private DependenciesBuilder(String firstOrigin) {
            this.currentOrigin = firstOrigin;
        }

        public DependenciesBuilder toNodes(String... names) {
            for (String name : names) {
                dependencies.add(new TestDependency(currentOrigin, name));
            }
            return this;
        }

        public DependenciesBuilder fromNode(String name) {
            currentOrigin = name;
            return this;
        }
    }

    private static class TestDependency {
        private final String origin;
        private final String target;

        private TestDependency(String origin, String target) {
            this.origin = origin;
            this.target = target;
        }

        @Override
        public String toString() {
            return "[" + origin + "] --> [" + target + "]";
        }
    }
}
