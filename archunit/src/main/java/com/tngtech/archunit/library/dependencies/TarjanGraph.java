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

import java.util.Arrays;

import com.tngtech.archunit.library.dependencies.PrimitiveDataTypes.IntArray;
import com.tngtech.archunit.library.dependencies.PrimitiveDataTypes.IntStack;

import static java.util.Arrays.fill;

/**
 * Extends a {@link PrimitiveGraph} with any data structures necessary to execute Tarjan's algorithm.
 * The idea is mainly to keep the code readable and understandable while using many primitive data structures
 * for performance reasons.
 */
class TarjanGraph {
    /**
     * Performance optimization, compare {@link #popStackUntilEncountering(int)}.
     */
    static final int[] LESS_THAN_TWO_VALUES = new int[0];

    private final PrimitiveGraph graph;
    /**
     * The order in which we encounter nodes of the {@link PrimitiveGraph} while doing a depth first search.
     * The index of the entry represents the respective node.
     */
    private final IntArray visitationIndexes;
    /**
     * All {@code lowLink} properties of the nodes we have encountered so far. A {@code lowLink} property as an invariant
     * will always be the lowest {@code visitationIndex} reachable from the current node with respect to the nodes we have
     * visited within the graph at any given moment.
     * The index of the entry represents the respective node.
     */
    private final IntArray lowLinks;
    /**
     * A stack containing all nodes we have visited in the current run. If we ever return from exploring all descendants
     * of a node and our {@code lowLink} property equals our {@code visitationIndex}, we know that all the nodes visited
     * thereafter that are still on the stack including our current node will form a strongly connected component.
     * Note that this might only include our current node (i.e. scc of size 1), which we will then ignore.
     */
    private final IntStack nodeStack;
    /**
     * Performance optimization to make checks if a node is on the stack quicker. We will redundantly track here if a node
     * is currently on the stack.
     * The index of the entry represents the respective node.
     */
    private final boolean[] nodeOnStack;
    /**
     * Performance optimization. When popping the current stack down to a specific node, we initially do not know how many
     * nodes we will return. Thus we need to temporarily store these nodes in some array to copy that array with the correct
     * size in the end. Note that allocating a new temporary array on each method call will make a <b>huge</b> difference
     * with regard to performance.
     */
    private final int[] tempArray;

    private TarjanGraph(PrimitiveGraph graph) {
        this.graph = graph;
        visitationIndexes = new IntArray(graph.getSize());
        lowLinks = new IntArray(graph.getSize());
        nodeStack = new IntStack(graph.getSize());
        nodeOnStack = new boolean[graph.getSize()];
        tempArray = new int[graph.getSize()];
    }

    int getSize() {
        return graph.getSize();
    }

    int[] getAdjacentNodesOf(int nodeIndex) {
        return graph.getAdjacentNodesOf(nodeIndex);
    }

    boolean isVisitationIndexUnset(int nodeIndex) {
        return !visitationIndexes.isSet(nodeIndex);
    }

    int getNodeVisitationIndex(int nodeIndex) {
        return visitationIndexes.get(nodeIndex);
    }

    void setNodeVisitationIndex(int nodeIndex, int visitationIndex) {
        visitationIndexes.set(nodeIndex, visitationIndex);
    }

    int getLowLink(int nodeIndex) {
        return lowLinks.get(nodeIndex);
    }

    void setLowLink(int nodeIndex, int lowLink) {
        lowLinks.set(nodeIndex, lowLink);
    }

    boolean isOnStack(int nodeIndex) {
        return nodeOnStack[nodeIndex];
    }

    void pushOnStack(int nodeIndex) {
        nodeStack.push(nodeIndex);
        nodeOnStack[nodeIndex] = true;
    }

    /**
     * Pops and returns all values on the stack until {@code nodeToVisit} is reached.
     * {@code nodeToVisit} will be part of the result.
     * Since we are always only interested in strongly connected components of size > 1,
     * we will return the constant {@link #LESS_THAN_TWO_VALUES} if we encountered less than two nodes
     * until reaching {@code nodeToVisit} as a performance optimization,
     * instead of uselessly copying the array.
     */
    int[] popStackUntilEncountering(int nodeToVisit) {
        int index = 0;
        int current;
        do {
            current = nodeStack.pop();
            nodeOnStack[current] = false;
            tempArray[index++] = current;
        } while (current != nodeToVisit);

        return index > 1 ? Arrays.copyOf(tempArray, index) : LESS_THAN_TWO_VALUES;
    }

    void reset() {
        visitationIndexes.reset();
        lowLinks.reset();
        nodeStack.reset();
        fill(nodeOnStack, false);
    }

    static TarjanGraph of(PrimitiveGraph graph) {
        return new TarjanGraph(graph);
    }
}
