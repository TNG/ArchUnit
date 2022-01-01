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

/**
 * An optimized graph stripped down to the bare minimum for cycle detection.
 * We represent nodes as integers from 0 ..< graph.size() and an edge as an array of two integers (node origin and node target).
 * To further optimize we represent all edges of a graph as a 2-dim array where the first index represents the node origin and
 * all entries for this index represent node targets, e.g.
 * {1 -> 2}, {1 -> 3}, {3 -> 5} => edges == [[], [2, 3], [], [5]]
 */
class PrimitiveGraph {
    private final int[][] edges;

    PrimitiveGraph(int[][] edges) {
        this.edges = edges;
    }

    int getSize() {
        return edges.length;
    }

    int[] getAdjacentNodesOf(int nodeIndex) {
        return edges[nodeIndex];
    }
}
