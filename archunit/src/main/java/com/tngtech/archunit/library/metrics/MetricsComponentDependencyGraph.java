/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;

class MetricsComponentDependencyGraph<T> {
    private final SetMultimap<MetricsComponent<T>, MetricsComponent<T>> outgoingComponentDependencies;
    private final SetMultimap<MetricsComponent<T>, MetricsComponent<T>> incomingComponentDependencies;
    private final Map<MetricsComponent<T>, Set<MetricsComponent<T>>> transitiveComponentDependencies;

    private MetricsComponentDependencyGraph(Iterable<MetricsComponent<T>> components, Function<T, Collection<T>> getDependencies) {
        ImmutableList<MetricsComponent<T>> componentList = ImmutableList.copyOf(components);
        ImmutableSetMultimap<MetricsComponent<T>, MetricsComponent<T>> componentDependencies = createComponentDependencies(componentList, getDependencies);
        this.outgoingComponentDependencies = componentDependencies;
        this.incomingComponentDependencies = componentDependencies.inverse();
        this.transitiveComponentDependencies = new TransitiveDependencyPrecomputation<>(componentList, componentDependencies).getTransitiveDependenciesByComponent();
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
        return transitiveComponentDependencies.getOrDefault(origin, ImmutableSet.of());
    }

    static <T> MetricsComponentDependencyGraph<T> of(Iterable<MetricsComponent<T>> components, Function<T, Collection<T>> getDependencies) {
        return new MetricsComponentDependencyGraph<>(components, getDependencies);
    }

    private static final class TransitiveDependencyPrecomputation<T> {
        private final ImmutableList<MetricsComponent<T>> components;
        private final int[][] outgoingEdgesByComponentIndex;

        private TransitiveDependencyPrecomputation(
                ImmutableList<MetricsComponent<T>> components,
                ImmutableSetMultimap<MetricsComponent<T>, MetricsComponent<T>> outgoingComponentDependencies
        ) {
            this.components = components;

            Map<MetricsComponent<T>, Integer> componentIndexByComponent = new HashMap<>();
            for (int i = 0; i < components.size(); i++) {
                componentIndexByComponent.put(components.get(i), i);
            }

            outgoingEdgesByComponentIndex = new int[components.size()][];

            for (int i = 0; i < components.size(); i++) {
                MetricsComponent<T> component = components.get(i);
                outgoingEdgesByComponentIndex[i] = toIndexes(outgoingComponentDependencies.get(component), componentIndexByComponent);
            }
        }

        private int[] toIndexes(Collection<MetricsComponent<T>> components, Map<MetricsComponent<T>, Integer> componentIndexByComponent) {
            int[] result = new int[components.size()];
            int index = 0;
            for (MetricsComponent<T> component : components) {
                result[index++] = componentIndexByComponent.get(component);
            }
            return result;
        }

        private Map<MetricsComponent<T>, Set<MetricsComponent<T>>> getTransitiveDependenciesByComponent() {
            // Collapse cycles first, then compute reachability once on the condensed DAG.
            StronglyConnectedComponents stronglyConnectedComponents = determineStronglyConnectedComponents();
            CondensedGraph condensedGraph = condense(stronglyConnectedComponents);
            BitSet[] reachableStronglyConnectedComponents = determineReachableStronglyConnectedComponents(condensedGraph);
            return mapToTransitiveDependenciesByComponent(stronglyConnectedComponents, reachableStronglyConnectedComponents);
        }

        private StronglyConnectedComponents determineStronglyConnectedComponents() {
            int[] indices = new int[components.size()];
            Arrays.fill(indices, -1);
            int[] lowlink = new int[components.size()];
            boolean[] onStack = new boolean[components.size()];
            Deque<Integer> stack = new ArrayDeque<>();
            List<int[]> componentIndexesByStronglyConnectedComponentIndex = new ArrayList<>();
            boolean[] isStronglyConnectedComponentSelfReachable = new boolean[components.size()];
            int[] stronglyConnectedComponentIndexByComponentIndex = new int[components.size()];
            Arrays.fill(stronglyConnectedComponentIndexByComponentIndex, -1);

            int[] timer = new int[1];

            for (int i = 0; i < components.size(); i++) {
                if (indices[i] == -1) {
                    strongConnect(i, indices, lowlink, onStack, stack, componentIndexesByStronglyConnectedComponentIndex,
                            isStronglyConnectedComponentSelfReachable, stronglyConnectedComponentIndexByComponentIndex, timer);
                }
            }

            return new StronglyConnectedComponents(
                    stronglyConnectedComponentIndexByComponentIndex,
                    componentIndexesByStronglyConnectedComponentIndex,
                    isStronglyConnectedComponentSelfReachable,
                    componentIndexesByStronglyConnectedComponentIndex.size()
            );
        }

        private void strongConnect(
                int v,
                int[] indices,
                int[] lowlink,
                boolean[] onStack,
                Deque<Integer> stack,
                List<int[]> componentIndexesByStronglyConnectedComponentIndex,
                boolean[] isStronglyConnectedComponentSelfReachable,
                int[] stronglyConnectedComponentIndexByComponentIndex,
                int[] timer
        ) {
            indices[v] = lowlink[v] = timer[0]++;
            stack.push(v);
            onStack[v] = true;

            for (int w : outgoingEdgesByComponentIndex[v]) {
                if (indices[w] == -1) {
                    strongConnect(w, indices, lowlink, onStack, stack, componentIndexesByStronglyConnectedComponentIndex,
                            isStronglyConnectedComponentSelfReachable, stronglyConnectedComponentIndexByComponentIndex, timer);
                    lowlink[v] = Math.min(lowlink[v], lowlink[w]);
                } else if (onStack[w]) {
                    lowlink[v] = Math.min(lowlink[v], indices[w]);
                }
            }

            if (lowlink[v] == indices[v]) {
                IntCollector currentStronglyConnectedComponent = new IntCollector();
                int w;
                do {
                    w = stack.pop();
                    onStack[w] = false;
                    stronglyConnectedComponentIndexByComponentIndex[w] = componentIndexesByStronglyConnectedComponentIndex.size();
                    currentStronglyConnectedComponent.add(w);
                } while (w != v);

                int[] stronglyConnectedComponent = currentStronglyConnectedComponent.toArray();
                componentIndexesByStronglyConnectedComponentIndex.add(stronglyConnectedComponent);
                isStronglyConnectedComponentSelfReachable[componentIndexesByStronglyConnectedComponentIndex.size() - 1] =
                        stronglyConnectedComponent.length > 1 || containsSelfLoop(stronglyConnectedComponent[0]);
            }
        }

        private boolean containsSelfLoop(int componentIndex) {
            for (int dependencyIndex : outgoingEdgesByComponentIndex[componentIndex]) {
                if (dependencyIndex == componentIndex) {
                    return true;
                }
            }
            return false;
        }

        private CondensedGraph condense(StronglyConnectedComponents stronglyConnectedComponents) {
            List<Set<Integer>> outgoingStronglyConnectedComponents = new ArrayList<>();
            int[] incomingDegreeByStronglyConnectedComponentIndex = new int[stronglyConnectedComponents.count];
            for (int i = 0; i < stronglyConnectedComponents.count; i++) {
                outgoingStronglyConnectedComponents.add(new HashSet<>());
            }

            for (int componentIndex = 0; componentIndex < components.size(); componentIndex++) {
                int originStronglyConnectedComponentIndex = stronglyConnectedComponents.stronglyConnectedComponentIndexByComponentIndex[componentIndex];
                for (int dependencyIndex : outgoingEdgesByComponentIndex[componentIndex]) {
                    int targetStronglyConnectedComponentIndex = stronglyConnectedComponents.stronglyConnectedComponentIndexByComponentIndex[dependencyIndex];
                    if (originStronglyConnectedComponentIndex != targetStronglyConnectedComponentIndex
                            && outgoingStronglyConnectedComponents.get(originStronglyConnectedComponentIndex).add(targetStronglyConnectedComponentIndex)) {
                        incomingDegreeByStronglyConnectedComponentIndex[targetStronglyConnectedComponentIndex]++;
                    }
                }
            }

            int[][] outgoingEdgesByStronglyConnectedComponentIndex = new int[stronglyConnectedComponents.count][];
            for (int i = 0; i < stronglyConnectedComponents.count; i++) {
                outgoingEdgesByStronglyConnectedComponentIndex[i] = toArray(outgoingStronglyConnectedComponents.get(i));
            }
            return new CondensedGraph(outgoingEdgesByStronglyConnectedComponentIndex, incomingDegreeByStronglyConnectedComponentIndex);
        }

        private int[] toArray(Set<Integer> values) {
            int[] result = new int[values.size()];
            int index = 0;
            for (Integer value : values) {
                result[index++] = value;
            }
            return result;
        }

        private BitSet[] determineReachableStronglyConnectedComponents(CondensedGraph condensedGraph) {
            int[] topologicalOrder = determineTopologicalOrder(condensedGraph);
            BitSet[] reachableStronglyConnectedComponentsByIndex = new BitSet[condensedGraph.outgoingEdgesByStronglyConnectedComponentIndex.length];

            for (int i = topologicalOrder.length - 1; i >= 0; i--) {
                int stronglyConnectedComponentIndex = topologicalOrder[i];
                BitSet reachable = new BitSet(condensedGraph.outgoingEdgesByStronglyConnectedComponentIndex.length);
                for (int dependencyIndex : condensedGraph.outgoingEdgesByStronglyConnectedComponentIndex[stronglyConnectedComponentIndex]) {
                    reachable.set(dependencyIndex);
                    reachable.or(reachableStronglyConnectedComponentsByIndex[dependencyIndex]);
                }
                reachableStronglyConnectedComponentsByIndex[stronglyConnectedComponentIndex] = reachable;
            }
            return reachableStronglyConnectedComponentsByIndex;
        }

        private int[] determineTopologicalOrder(CondensedGraph condensedGraph) {
            int[] remainingIncomingDegree = Arrays.copyOf(
                    condensedGraph.incomingDegreeByStronglyConnectedComponentIndex,
                    condensedGraph.incomingDegreeByStronglyConnectedComponentIndex.length
            );
            int[] topologicalOrder = new int[remainingIncomingDegree.length];
            Deque<Integer> roots = new ArrayDeque<>();
            for (int i = 0; i < remainingIncomingDegree.length; i++) {
                if (remainingIncomingDegree[i] == 0) {
                    roots.add(i);
                }
            }

            int currentIndex = 0;
            while (!roots.isEmpty()) {
                int currentStronglyConnectedComponentIndex = roots.removeFirst();
                topologicalOrder[currentIndex++] = currentStronglyConnectedComponentIndex;
                for (int dependencyIndex : condensedGraph.outgoingEdgesByStronglyConnectedComponentIndex[currentStronglyConnectedComponentIndex]) {
                    remainingIncomingDegree[dependencyIndex]--;
                    if (remainingIncomingDegree[dependencyIndex] == 0) {
                        roots.addLast(dependencyIndex);
                    }
                }
            }
            return topologicalOrder;
        }

        private Map<MetricsComponent<T>, Set<MetricsComponent<T>>> mapToTransitiveDependenciesByComponent(
                StronglyConnectedComponents stronglyConnectedComponents,
                BitSet[] reachableStronglyConnectedComponentsByIndex
        ) {
            ImmutableMap.Builder<MetricsComponent<T>, Set<MetricsComponent<T>>> result = ImmutableMap.builder();
            List<ImmutableSet<MetricsComponent<T>>> transitiveDependenciesByStronglyConnectedComponentIndex = new ArrayList<>(stronglyConnectedComponents.count);

            for (int stronglyConnectedComponentIndex = 0; stronglyConnectedComponentIndex < stronglyConnectedComponents.count; stronglyConnectedComponentIndex++) {
                BitSet reachableStronglyConnectedComponents = (BitSet) reachableStronglyConnectedComponentsByIndex[stronglyConnectedComponentIndex].clone();
                if (stronglyConnectedComponents.isSelfReachable[stronglyConnectedComponentIndex]) {
                    reachableStronglyConnectedComponents.set(stronglyConnectedComponentIndex);
                }
                transitiveDependenciesByStronglyConnectedComponentIndex.add(
                        mapToComponents(reachableStronglyConnectedComponents, stronglyConnectedComponents.componentIndexesByStronglyConnectedComponentIndex)
                );
            }

            for (int componentIndex = 0; componentIndex < components.size(); componentIndex++) {
                int stronglyConnectedComponentIndex = stronglyConnectedComponents.stronglyConnectedComponentIndexByComponentIndex[componentIndex];
                result.put(components.get(componentIndex), transitiveDependenciesByStronglyConnectedComponentIndex.get(stronglyConnectedComponentIndex));
            }
            return result.build();
        }

        private ImmutableSet<MetricsComponent<T>> mapToComponents(
                BitSet reachableStronglyConnectedComponents,
                List<int[]> componentIndexesByStronglyConnectedComponentIndex
        ) {
            ImmutableSet.Builder<MetricsComponent<T>> result = ImmutableSet.builder();
            for (int stronglyConnectedComponentIndex = reachableStronglyConnectedComponents.nextSetBit(0);
                 stronglyConnectedComponentIndex >= 0;
                 stronglyConnectedComponentIndex = reachableStronglyConnectedComponents.nextSetBit(stronglyConnectedComponentIndex + 1)) {
                for (int componentIndex : componentIndexesByStronglyConnectedComponentIndex.get(stronglyConnectedComponentIndex)) {
                    result.add(components.get(componentIndex));
                }
            }
            return result.build();
        }
    }

    private static final class StronglyConnectedComponents {
        private final int[] stronglyConnectedComponentIndexByComponentIndex;
        private final List<int[]> componentIndexesByStronglyConnectedComponentIndex;
        private final boolean[] isSelfReachable;
        private final int count;

        private StronglyConnectedComponents(
                int[] stronglyConnectedComponentIndexByComponentIndex,
                List<int[]> componentIndexesByStronglyConnectedComponentIndex,
                boolean[] isSelfReachable,
                int count
        ) {
            this.stronglyConnectedComponentIndexByComponentIndex = stronglyConnectedComponentIndexByComponentIndex;
            this.componentIndexesByStronglyConnectedComponentIndex = componentIndexesByStronglyConnectedComponentIndex;
            this.isSelfReachable = isSelfReachable;
            this.count = count;
        }
    }

    private static final class CondensedGraph {
        private final int[][] outgoingEdgesByStronglyConnectedComponentIndex;
        private final int[] incomingDegreeByStronglyConnectedComponentIndex;

        private CondensedGraph(int[][] outgoingEdgesByStronglyConnectedComponentIndex, int[] incomingDegreeByStronglyConnectedComponentIndex) {
            this.outgoingEdgesByStronglyConnectedComponentIndex = outgoingEdgesByStronglyConnectedComponentIndex;
            this.incomingDegreeByStronglyConnectedComponentIndex = incomingDegreeByStronglyConnectedComponentIndex;
        }
    }

    private static final class IntCollector {
        private int[] values = new int[4];
        private int size = 0;

        private void add(int value) {
            if (size == values.length) {
                values = Arrays.copyOf(values, values.length * 2);
            }
            values[size++] = value;
        }

        private int[] toArray() {
            return Arrays.copyOf(values, size);
        }
    }
}
