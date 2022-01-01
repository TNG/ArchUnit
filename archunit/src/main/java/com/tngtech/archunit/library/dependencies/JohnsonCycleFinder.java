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
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.tngtech.archunit.library.dependencies.CycleConfiguration.MAX_NUMBER_OF_CYCLES_TO_DETECT_PROPERTY_NAME;
import static com.tngtech.archunit.library.dependencies.TarjanComponentFinder.NO_COMPONENT_FOUND;

/**
 * An implementation of Johnson's algorithm to find cycles within an uni-directed graph
 * (cf. Johnson, <i>Finding all Elementary Cycles in a Directed Graph</i>, Siam J. Computing Vol. 4 No. 1 March 1975)
 * <br><br>
 * The idea is to iterate the graph (represented as integer nodes and edges between two integer nodes)
 * and for each node index find the next strongly connected component with the lowest index.
 * Once found we will look for (elementary) cycles within this strongly connected component. Obviously we are thus
 * only ever interested in strongly connected components of size greater than 1. We take the lowest node index
 * of the strongly connected component as our starting point and only look for paths through the strongly
 * connected component that lead back to this starting point. Once we found all cycles through the starting node this way,
 * we remove the starting node from the graph (look at the induced sub graph of node indexes larger than the staring
 * node index) and repeat the process (i.e. find the next strongly connected component within that graph and find
 * all cycles through the starting node).
 * To make this process efficient, Johnson uses a couple of data structures, represented within {@link JohnsonComponent}.
 * Notably we
 * <ul>
 *     <li>put each node we visit on a stack so if we find back to our starting node
 *         we can pop the stack and thus have a cycle</li>
 *     <li>we record each node we visit in a blocked set so we do not visit nodes twice
 *         of which we know they cannot lead back to the starting node</li>
 *     <li>we keep a map of dependently blocked node indexes. If we run into a dead end,
 *         because the target node we want to visit through an edge is already blocked,
 *         we record this in this map. If we find a cycle and the target node gets unblocked,
 *         we will then also unblock this node to open the possibility to find another cycle
 *         (if the target node will never be unblocked we have not found a cycle through
 *         this node and thus there cannot be a way back from this node to the starting node.
 *         We then also never need to unblock this node, if all its descendants cannot lead
 *         back to the starting node)</li>
 * </ul>
 */
class JohnsonCycleFinder {
    private static final Logger log = LoggerFactory.getLogger(JohnsonCycleFinder.class);

    private int nodeToProcess = 0;
    private final PrimitiveGraph primitiveGraph;

    JohnsonCycleFinder(PrimitiveGraph primitiveGraph) {
        this.primitiveGraph = primitiveGraph;
    }

    Result findCycles() {
        Result result = new Result();
        TarjanComponentFinder componentFinder = new TarjanComponentFinder(primitiveGraph);
        JohnsonComponent johnsonComponent = JohnsonComponent.within(primitiveGraph);
        while (nodeToProcess < primitiveGraph.getSize()) {
            int[] nextStronglyConnectedComponent = componentFinder.findNonTrivialStronglyConnectedComponentWithLowestNodeIndexAbove(nodeToProcess);
            if (nextStronglyConnectedComponent == NO_COMPONENT_FOUND) {
                break;
            }

            johnsonComponent.init(nextStronglyConnectedComponent);
            findCycles(result, johnsonComponent.getStartNodeIndex(), johnsonComponent);
            nodeToProcess = johnsonComponent.getStartNodeIndex() + 1;
        }
        return result;
    }

    private boolean findCycles(Result result, int originNodeIndex, JohnsonComponent johnsonComponent) {
        if (!result.canAcceptMoreCycles()) {
            return false;
        }

        boolean foundCycle = false;
        johnsonComponent.pushOnStack(originNodeIndex);
        johnsonComponent.block(originNodeIndex);

        int[] targetNodeIndexes = johnsonComponent.getAdjacentNodesOf(originNodeIndex);
        for (int targetNodeIndex : targetNodeIndexes) {
            if (johnsonComponent.isStartNodeIndex(targetNodeIndex)) {
                result.add(johnsonComponent.getStack());
                foundCycle = true;
            } else if (johnsonComponent.isNotBlocked(targetNodeIndex)) {
                foundCycle = foundCycle | findCycles(result, targetNodeIndex, johnsonComponent);
            }
        }

        if (foundCycle) {
            johnsonComponent.unblock(originNodeIndex);
        } else {
            for (int targetNodeIndex : targetNodeIndexes) {
                johnsonComponent.markDependentlyBlocked(originNodeIndex, targetNodeIndex);
            }
        }

        johnsonComponent.popFromStack();
        return foundCycle;
    }

    static class Result implements Iterable<int[]> {
        private final CycleConfiguration configuration = new CycleConfiguration();
        private List<int[]> cycles = new ArrayList<>();
        private boolean maxNumberOfCyclesReached = false;

        private Result() {
            log.debug("Maximum number of cycles to detect is set to {}; "
                            + "this limit can be adapted using the `archunit.properties` value `{}=xxx`",
                    configuration.getMaxNumberOfCyclesToDetect(), MAX_NUMBER_OF_CYCLES_TO_DETECT_PROPERTY_NAME);
        }

        private boolean canAcceptMoreCycles() {
            return !maxNumberOfCyclesReached;
        }

        boolean maxNumberOfCyclesReached() {
            return maxNumberOfCyclesReached;
        }

        void add(int[] cycle) {
            if (maxNumberOfCyclesReached) {
                return;
            }

            if (this.cycles.size() >= configuration.getMaxNumberOfCyclesToDetect()) {
                maxNumberOfCyclesReached = true;
                return;
            }

            this.cycles.add(cycle);
        }

        @Override
        public Iterator<int[]> iterator() {
            return cycles.iterator();
        }
    }
}
