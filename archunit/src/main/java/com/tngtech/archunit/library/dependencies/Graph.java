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

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ForwardingCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

import static com.google.common.base.Preconditions.checkArgument;

class Graph<T, ATTACHMENT> {
    private final Map<T, Integer> nodes = new HashMap<>();
    private final ListMultimap<Integer, Edge<T, ATTACHMENT>> outgoingEdges = ArrayListMultimap.create();

    void addNodes(Iterable<T> nodes) {
        for (T node : nodes) {
            if (!this.nodes.containsKey(node)) {
                this.nodes.put(node, this.nodes.size());
            }
        }
    }

    void addEdges(Iterable<Edge<T, ATTACHMENT>> outgoingEdges) {
        for (Edge<T, ATTACHMENT> edge : outgoingEdges) {
            checkArgument(nodes.containsKey(edge.getFrom()), "Node %s of edge %s is not part of the graph", edge.getFrom(), edge);
            checkArgument(nodes.containsKey(edge.getTo()), "Node %s of edge %s is not part of the graph", edge.getTo(), edge);
            this.outgoingEdges.put(nodes.get(edge.getFrom()), edge);
        }
    }

    Cycles<T, ATTACHMENT> findCycles() {
        Map<Integer, Map<Integer, Edge<T, ATTACHMENT>>> edgesByTargetIndexByOriginIndex = indexEdgesByTargetIndexByOriginIndex(nodes, outgoingEdges);
        JohnsonCycleFinder johnsonCycleFinder = new JohnsonCycleFinder(createPrimitiveGraph());
        ImmutableList.Builder<Cycle<T, ATTACHMENT>> result = ImmutableList.builder();
        JohnsonCycleFinder.Result cycles = johnsonCycleFinder.findCycles();
        for (int[] rawCycle : cycles) {
            result.add(mapToCycle(edgesByTargetIndexByOriginIndex, rawCycle));
        }
        return new Cycles<>(result.build(), cycles.maxNumberOfCyclesReached());
    }

    private PrimitiveGraph createPrimitiveGraph() {
        int[][] edges = new int[nodes.size()][];
        for (Map.Entry<T, Integer> nodeToIndex : nodes.entrySet()) {
            List<Edge<T, ATTACHMENT>> outgoing = outgoingEdges.get(nodeToIndex.getValue());
            edges[nodeToIndex.getValue()] = new int[outgoing.size()];
            for (int j = 0; j < outgoing.size(); j++) {
                edges[nodeToIndex.getValue()][j] = nodes.get(outgoing.get(j).getTo());
            }
        }
        return new PrimitiveGraph(edges);
    }

    private ImmutableMap<Integer, Map<Integer, Edge<T, ATTACHMENT>>> indexEdgesByTargetIndexByOriginIndex(
            Map<T, Integer> nodes,
            Multimap<Integer, Edge<T, ATTACHMENT>> outgoingEdges) {

        ImmutableMap.Builder<Integer, Map<Integer, Edge<T, ATTACHMENT>>> edgeMapBuilder = ImmutableMap.builder();
        for (Map.Entry<Integer, Collection<Edge<T, ATTACHMENT>>> originIndexToEdges : outgoingEdges.asMap().entrySet()) {
            ImmutableMap.Builder<Integer, Edge<T, ATTACHMENT>> targetIndexToEdges = ImmutableMap.builder();
            for (Edge<T, ATTACHMENT> edge : originIndexToEdges.getValue()) {
                targetIndexToEdges.put(nodes.get(edge.getTo()), edge);
            }
            edgeMapBuilder.put(originIndexToEdges.getKey(), targetIndexToEdges.build());
        }
        return edgeMapBuilder.build();
    }

    private Cycle<T, ATTACHMENT> mapToCycle(Map<Integer, Map<Integer, Edge<T, ATTACHMENT>>> edgesByTargetIndexByOriginIndex, int[] rawCycle) {
        ImmutableList.Builder<Edge<T, ATTACHMENT>> edges = ImmutableList.builder();
        int originIndex = -1;
        for (int targetIndex : rawCycle) {
            if (originIndex >= 0) {
                edges.add(edgesByTargetIndexByOriginIndex.get(originIndex).get(targetIndex));
            }
            originIndex = targetIndex;
        }
        edges.add(edgesByTargetIndexByOriginIndex.get(originIndex).get(rawCycle[0]));
        return new Cycle<>(edges.build());
    }

    @Override
    public String toString() {
        return "Graph{" +
                "nodes=" + nodes +
                ", edges=" + outgoingEdges +
                '}';
    }

    static class Cycles<T, ATTACHMENT> extends ForwardingCollection<Cycle<T, ATTACHMENT>> {
        private final Collection<Cycle<T, ATTACHMENT>> cycles;
        private final boolean maxNumberOfCyclesReached;

        private Cycles(Collection<Cycle<T, ATTACHMENT>> cycles, boolean maxNumberOfCyclesReached) {
            this.cycles = cycles;
            this.maxNumberOfCyclesReached = maxNumberOfCyclesReached;
        }

        boolean maxNumberOfCyclesReached() {
            return maxNumberOfCyclesReached;
        }

        @Override
        protected Collection<Cycle<T, ATTACHMENT>> delegate() {
            return cycles;
        }
    }
}
