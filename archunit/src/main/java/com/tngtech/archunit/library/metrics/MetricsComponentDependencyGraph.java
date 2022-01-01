/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.library.metrics;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;
import com.tngtech.archunit.base.Function;

class MetricsComponentDependencyGraph<T> {
    private final SetMultimap<MetricsComponent<T>, MetricsComponent<T>> outgoingComponentDependencies;
    private final SetMultimap<MetricsComponent<T>, MetricsComponent<T>> incomingComponentDependencies;

    private MetricsComponentDependencyGraph(Iterable<MetricsComponent<T>> components, Function<T, Collection<T>> getDependencies) {
        ImmutableSetMultimap<MetricsComponent<T>, MetricsComponent<T>> componentDependencies = createComponentDependencies(components, getDependencies);
        this.outgoingComponentDependencies = componentDependencies;
        this.incomingComponentDependencies = componentDependencies.inverse();
    }

    private ImmutableSetMultimap<MetricsComponent<T>, MetricsComponent<T>> createComponentDependencies(Iterable<MetricsComponent<T>> components, Function<T, Collection<T>> getDependencies) {
        Map<T, MetricsComponent<T>> componentsByElements = indexComponentByElement(components);
        ImmutableSetMultimap.Builder<MetricsComponent<T>, MetricsComponent<T>> componentDependencies = ImmutableSetMultimap.builder();
        for (MetricsComponent<T> component : components) {
            componentDependencies.putAll(component, createDependenciesOf(component, componentsByElements, getDependencies));
        }
        return componentDependencies.build();
    }

    private Map<T, MetricsComponent<T>> indexComponentByElement(Iterable<MetricsComponent<T>> components) {
        Map<T, MetricsComponent<T>> componentsByElements = new HashMap<>();
        for (MetricsComponent<T> component : components) {
            for (T element : component.getElements()) {
                componentsByElements.put(element, component);
            }
        }
        return componentsByElements;
    }

    private ImmutableSet<MetricsComponent<T>> createDependenciesOf(MetricsComponent<T> component, Map<T, MetricsComponent<T>> componentsByElements, Function<T, Collection<T>> getDependencies) {
        ImmutableSet.Builder<MetricsComponent<T>> builder = ImmutableSet.builder();
        for (T element : component.getElements()) {
            for (T dependency : getDependencies.apply(element)) {
                MetricsComponent<T> target = componentsByElements.get(dependency);
                if (target != null && !target.equals(component)) {
                    builder.add(target);
                }
            }
        }
        return builder.build();
    }

    Set<MetricsComponent<T>> getDirectDependenciesFrom(MetricsComponent<T> origin) {
        return outgoingComponentDependencies.get(origin);
    }

    Set<MetricsComponent<T>> getDirectDependenciesTo(MetricsComponent<T> target) {
        return incomingComponentDependencies.get(target);
    }

    Set<MetricsComponent<T>> getTransitiveDependenciesOf(MetricsComponent<T> origin) {
        ImmutableSet.Builder<MetricsComponent<T>> transitiveDependencies = ImmutableSet.builder();
        Set<MetricsComponent<T>> analyzedComponents = new HashSet<>();  // to avoid infinite recursion for cyclic dependencies
        addTransitiveDependenciesFrom(origin, transitiveDependencies, analyzedComponents);
        return transitiveDependencies.build();
    }

    private void addTransitiveDependenciesFrom(MetricsComponent<T> component, ImmutableSet.Builder<MetricsComponent<T>> transitiveDependencies, Set<MetricsComponent<T>> analyzedComponents) {
        analyzedComponents.add(component);  // currently being analyzed
        Set<MetricsComponent<T>> dependencyTargetsToRecurse = new HashSet<>();
        for (MetricsComponent<T> dependency : getDirectDependenciesFrom(component)) {
            transitiveDependencies.add(dependency);
            dependencyTargetsToRecurse.add(dependency);
        }
        for (MetricsComponent<T> dependency : dependencyTargetsToRecurse) {
            if (!analyzedComponents.contains(dependency)) {
                addTransitiveDependenciesFrom(dependency, transitiveDependencies, analyzedComponents);
            }
        }
    }

    static <T> MetricsComponentDependencyGraph<T> of(Iterable<MetricsComponent<T>> components, Function<T, Collection<T>> getDependencies) {
        return new MetricsComponentDependencyGraph<>(components, getDependencies);
    }
}
