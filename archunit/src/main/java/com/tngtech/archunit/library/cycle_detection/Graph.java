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
package com.tngtech.archunit.library.cycle_detection;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.tngtech.archunit.base.ForwardingCollection;

import static com.google.common.base.Preconditions.checkArgument;

class Graph<NODE, EDGE extends Edge<NODE>> {
    private final Map<NODE, Integer> nodes = new HashMap<>();
    private final ListMultimap<Integer, EDGE> outgoingEdges = ArrayListMultimap.create();

    void addNodes(Collection<NODE> nodes) {
        for (NODE node : nodes) {
            if (!this.nodes.containsKey(node)) {
                this.nodes.put(node, this.nodes.size());
            }
        }
    }

    void addEdges(Iterable<EDGE> outgoingEdges) {
        for (EDGE edge : outgoingEdges) {
            checkArgument(nodes.containsKey(edge.getOrigin()), "Node %s of edge %s is not part of the graph", edge.getOrigin(), edge);
            checkArgument(nodes.containsKey(edge.getTarget()), "Node %s of edge %s is not part of the graph", edge.getTarget(), edge);
            this.outgoingEdges.put(nodes.get(edge.getOrigin()), edge);
        }
    }

    Cycles<EDGE> findCycles() {
        JohnsonCycleFinder johnsonCycleFinder = new JohnsonCycleFinder(createPrimitiveGraph());
        JohnsonCycleFinder.Result rawCycles = johnsonCycleFinder.findCycles();
        return new CyclesInternal<>(mapToCycles(rawCycles), rawCycles.maxNumberOfCyclesReached());
    }

    private PrimitiveGraph createPrimitiveGraph() {
        int[][] edges = new int[nodes.size()][];
        for (Map.Entry<NODE, Integer> nodeToIndex : nodes.entrySet()) {
            List<EDGE> outgoing = outgoingEdges.get(nodeToIndex.getValue());
            edges[nodeToIndex.getValue()] = new int[outgoing.size()];
            for (int j = 0; j < outgoing.size(); j++) {
                edges[nodeToIndex.getValue()][j] = nodes.get(outgoing.get(j).getTarget());
            }
        }
        return new PrimitiveGraph(edges);
    }

    private ImmutableList<Cycle<EDGE>> mapToCycles(JohnsonCycleFinder.Result rawCycles) {
        Map<Integer, Map<Integer, EDGE>> edgesByTargetIndexByOriginIndex = indexEdgesByTargetIndexByOriginIndex(nodes, outgoingEdges);
        ImmutableList.Builder<Cycle<EDGE>> result = ImmutableList.builder();
        for (int[] rawCycle : rawCycles) {
            result.add(mapToCycle(edgesByTargetIndexByOriginIndex, rawCycle));
        }
        return result.build();
    }

    private ImmutableMap<Integer, Map<Integer, EDGE>> indexEdgesByTargetIndexByOriginIndex(
            Map<NODE, Integer> nodes,
            Multimap<Integer, EDGE> outgoingEdges) {

        ImmutableMap.Builder<Integer, Map<Integer, EDGE>> edgeMapBuilder = ImmutableMap.builder();
        for (Map.Entry<Integer, Collection<EDGE>> originIndexToEdges : outgoingEdges.asMap().entrySet()) {
            ImmutableMap.Builder<Integer, EDGE> targetIndexToEdges = ImmutableMap.builder();
            for (EDGE edge : originIndexToEdges.getValue()) {
                targetIndexToEdges.put(nodes.get(edge.getTarget()), edge);
            }
            edgeMapBuilder.put(originIndexToEdges.getKey(), targetIndexToEdges.build());
        }
        return edgeMapBuilder.build();
    }

    private Cycle<EDGE> mapToCycle(Map<Integer, Map<Integer, EDGE>> edgesByTargetIndexByOriginIndex, int[] rawCycle) {
        ImmutableList.Builder<EDGE> edges = ImmutableList.builder();
        int originIndex = -1;
        for (int targetIndex : rawCycle) {
            if (originIndex >= 0) {
                edges.add(edgesByTargetIndexByOriginIndex.get(originIndex).get(targetIndex));
            }
            originIndex = targetIndex;
        }
        edges.add(edgesByTargetIndexByOriginIndex.get(originIndex).get(rawCycle[0]));
        return new CycleInternal<>(edges.build());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "nodes=" + nodes +
                ", edges=" + outgoingEdges +
                '}';
    }

    private static class CyclesInternal<EDGE extends Edge<?>> extends ForwardingCollection<Cycle<EDGE>> implements Cycles<EDGE> {
        private final Collection<Cycle<EDGE>> cycles;
        private final boolean maxNumberOfCyclesReached;

        private CyclesInternal(Collection<Cycle<EDGE>> cycles, boolean maxNumberOfCyclesReached) {
            this.cycles = cycles;
            this.maxNumberOfCyclesReached = maxNumberOfCyclesReached;
        }

        @Override
        public boolean maxNumberOfCyclesReached() {
            return maxNumberOfCyclesReached;
        }

        @Override
        protected Collection<Cycle<EDGE>> delegate() {
            return cycles;
        }
    }
}
