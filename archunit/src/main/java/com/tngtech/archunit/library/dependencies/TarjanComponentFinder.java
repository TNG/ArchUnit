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
package com.tngtech.archunit.library.dependencies;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import com.google.common.primitives.Ints;

import static com.tngtech.archunit.library.dependencies.TarjanGraph.LESS_THAN_TWO_VALUES;
import static java.util.Arrays.sort;

/**
 * An implementation of Tarjan's algorithm to find strongly connected components
 * (compare <a href="https://en.wikipedia.org/wiki/Tarjan%27s_strongly_connected_components_algorithm">Tarjan's algorithm on Wikipedia</a>)
 * <br><br>
 * The idea is to do a depth first search through the graph. We start at some node {@code x}, then record the {@code visitationIndex} of any
 * node we encounter, i.e. the order in which we visit the node. Each node we encounter in a single run, we also put on a stack.
 * The important part is the {@code lowLink} property, which records the lowest {@code visitationIndex} we encountered while following
 * all descendants of this node. Initially this will be equal to the {@code visitationIndex}.
 * If we have ever explored all neighbors of a node doing the depth first search, and our own {@code lowLink} is still the same as
 * the {@code visitationIndex} we have encountered a strongly connected component. Note that while theoretically a single node is
 * a strongly connected component within itself, we are not interested in those trivial components and will skip them at the source.
 * <br><br>
 * Note that we keep track of all Tarjan specific state within {@link #graph}.
 * <br><br>
 * Also note that we always only need to find the strongly connected component containing the next unvisited node in ascending order.
 * Thus we do not need to find all strongly connected components, but only the next relevant one to apply Johnson's algorithm to.
 */
class TarjanComponentFinder {
    static final int[] NO_COMPONENT_FOUND = new int[0];

    private int nextIndex = 0;
    private final TarjanGraph graph;

    TarjanComponentFinder(PrimitiveGraph primitiveGraph) {
        graph = TarjanGraph.of(primitiveGraph);
    }

    private void reset() {
        nextIndex = 0;
        graph.reset();
    }

    /**
     * Returns a strongly connected component containing the next node larger than or equal to {@code lowerIndexBound}.<br>
     * The returned strongly connected component will only contain nodes with a node index larger than or equal to {@code lowerIndexBound}.<br>
     * If no such strongly connected component can be found {@link #NO_COMPONENT_FOUND} will be returned.<br>
     * Note that the returned array of node indexes is guaranteed to be sorted in ascending order.
     */
    int[] findNonTrivialStronglyConnectedComponentWithLowestNodeIndexAbove(int lowerIndexBound) {
        int[] nextComponent = findNonTrivialLowestStronglyConnectedComponentInSubGraphInducedByLowerBound(lowerIndexBound);
        reset();
        return nextComponent;
    }

    private int[] findNonTrivialLowestStronglyConnectedComponentInSubGraphInducedByLowerBound(int lowerIndexBound) {
        for (int j = lowerIndexBound; j < graph.getSize(); j++) {
            if (graph.isVisitationIndexUnset(j)) {
                List<int[]> components = findNonTrivialStronglyConnectedComponents(j, lowerIndexBound);
                if (!components.isEmpty()) {
                    return findComponentWithLowestNode(components);
                }
            }
        }
        return NO_COMPONENT_FOUND;
    }

    private List<int[]> findNonTrivialStronglyConnectedComponents(int nodeToVisit, int lowerIndexBound) {
        int currentIndex = nextIndex++;
        graph.setNodeVisitationIndex(nodeToVisit, currentIndex);
        graph.setLowLink(nodeToVisit, currentIndex);
        graph.pushOnStack(nodeToVisit);

        List<int[]> result = findNonTrivialStronglyConnectedComponentsOfDescendants(nodeToVisit, lowerIndexBound);

        // if lowlink is still equal to the visitation index, we have found the start of a strongly connected component
        if (graph.getLowLink(nodeToVisit) == graph.getNodeVisitationIndex(nodeToVisit)) {
            int[] currentStack = graph.popStackUntilEncountering(nodeToVisit);
            if (currentStack != LESS_THAN_TWO_VALUES) {
                result.add(currentStack);
            }
        }
        return result;
    }

    private List<int[]> findNonTrivialStronglyConnectedComponentsOfDescendants(int nodeToVisit, int lowerIndexBound) {
        List<int[]> result = new ArrayList<>();
        for (int targetNode : graph.getAdjacentNodesOf(nodeToVisit)) {
            if (targetNode < lowerIndexBound) {
                continue;
            }

            if (graph.isVisitationIndexUnset(targetNode)) {
                // we have not seen this node so far, so we will recursively search for strongly connected components
                result.addAll(findNonTrivialStronglyConnectedComponents(targetNode, lowerIndexBound));
                // then we can safely backtrack lowlink, i.e. set lowlink to the minimum of the target node lowlink and ours
                int newLowLink = Math.min(graph.getLowLink(nodeToVisit), graph.getLowLink(targetNode));
                graph.setLowLink(nodeToVisit, newLowLink);
            } else if (graph.isOnStack(targetNode)) {
                // we encountered a node of the same strongly connected component
                // to keep our invariant about lowlink, lowlink must now be the minimum of the current lowlink
                // and the visitation index of this target node
                int newLowLink = Math.min(graph.getNodeVisitationIndex(targetNode), graph.getLowLink(nodeToVisit));
                graph.setLowLink(nodeToVisit, newLowLink);
            }
        }
        return result;
    }

    private int[] findComponentWithLowestNode(List<int[]> component) {
        int[] componentWithLowestNodeIndex = Ordering.natural().onResultOf(MINIMUM_OF_INT_ARRAY).min(component);
        sort(componentWithLowestNodeIndex);
        return componentWithLowestNodeIndex;
    }

    private static final Function<int[], Integer> MINIMUM_OF_INT_ARRAY = new Function<int[], Integer>() {
        @Override
        public Integer apply(int[] input) {
            return Ints.min(input);
        }
    };
}
