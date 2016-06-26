package com.tngtech.archunit.library.dependencies;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singleton;

class Graph<T, ATTACHMENT> {
    private final Set<T> nodes = new HashSet<>();
    private final Multimap<T, Edge<T, ATTACHMENT>> outgoingEdges = HashMultimap.create();
    private final Set<Cycle<T, ATTACHMENT>> cycles = new HashSet<>();

    void add(T node, Set<Edge<T, ATTACHMENT>> connectingEdges) {
        nodes.add(checkNotNull(node));
        for (Edge<T, ATTACHMENT> edge : connectingEdges) {
            addEdge(edge);
        }
        cycles.addAll(cyclesThrough(node));
    }

    private void addEdge(Edge<T, ATTACHMENT> edge) {
        checkArgument(nodes.contains(edge.getFrom()), "Node %s of edge %s is not part of the graph", edge.getFrom(), edge);
        checkArgument(nodes.contains(edge.getTo()), "Node %s of edge %s is not part of the graph", edge.getTo(), edge);
        outgoingEdges.put(edge.getFrom(), edge);
    }

    Set<Cycle<T, ATTACHMENT>> getCycles() {
        return ImmutableSet.copyOf(cycles);
    }

    private Set<Cycle<T, ATTACHMENT>> cyclesThrough(T node) {
        Set<Cycle<T, ATTACHMENT>> result = new HashSet<>();
        for (Path<T, ATTACHMENT> pathThroughNode : follow(outgoingEdges.get(node), new Path<T, ATTACHMENT>(), singleton(node))) {
            if (pathThroughNode.isCycle()) {
                result.add(Cycle.from(pathThroughNode));
            }
        }
        return result;
    }

    private Set<Path<T, ATTACHMENT>> follow(Collection<Edge<T, ATTACHMENT>> edges, Path<T, ATTACHMENT> incomingPath, Set<T> visitedNodes) {
        Set<Path<T, ATTACHMENT>> result = new HashSet<>();
        for (Edge<T, ATTACHMENT> edge : edges) {
            result.addAll(follow(edge, incomingPath, visitedNodes));
        }
        return result;
    }

    private Set<Path<T, ATTACHMENT>> follow(Edge<T, ATTACHMENT> edge, Path<T, ATTACHMENT> incomingPath, Set<T> visitedNodes) {
        Path<T, ATTACHMENT> newPath = new Path<>(incomingPath).append(edge);
        if (visitedNodes.contains(newPath.getEnd())) {
            return singleton(newPath);
        }
        Set<T> nowVisited = union(visitedNodes, edge.getTo());
        return follow(outgoingEdges.get(edge.getTo()), newPath, nowVisited);
    }

    private Set<T> union(Set<T> set, T additionalElement) {
        return ImmutableSet.<T>builder().addAll(set).add(additionalElement).build();
    }

    @Override
    public String toString() {
        return "Graph{" +
                "nodes=" + nodes +
                ", edges=" + outgoingEdges.values() +
                '}';
    }
}
