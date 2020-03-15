package com.tngtech.archunit.library.dependencies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;

import static com.tngtech.archunit.library.dependencies.GraphTest.newEdge;

public class RealLifeGraph {
    // This graph has originally been taken from the toplevel packages of org.hibernate
    private static final Multimap<Integer, Integer> edgeTargetsByOrigin = createEdges();

    private static Multimap<Integer, Integer> createEdges() {
        Multimap<Integer, Integer> result = HashMultimap.create();
        result.putAll(0, ImmutableSet.of(33, 2, 12, 4, 16, 15, 42, 5, 10, 26, 39));
        result.putAll(1, ImmutableSet.of(3, 2, 16, 35, 27, 18, 7, 19, 39, 23, 42, 10, 44, 47, 26, 48));
        result.putAll(2, ImmutableSet.of(33, 0, 16, 35, 5, 18, 8, 36, 38, 39, 42, 10, 24, 25, 47, 26));
        result.putAll(3, ImmutableSet.of(42, 18, 7, 39));
        result.putAll(4, ImmutableSet.of(33, 2, 1, 12, 14, 16, 15, 17, 18, 8, 28, 19, 36, 38, 39, 30, 42, 10, 43, 45, 47, 26, 48));
        result.putAll(5, ImmutableSet.of(0, 16, 42, 10, 45, 21, 26));
        result.putAll(6, ImmutableSet.of(33, 16, 35, 5, 18, 28, 39, 23, 42, 10, 24, 45, 26));
        result.putAll(7, ImmutableSet.of(3, 16, 42, 5, 17, 18, 25, 39, 26));
        result.putAll(8, ImmutableSet.of(42, 7, 18));
        result.putAll(10, ImmutableSet.of(1, 0, 4, 15, 16, 27, 5, 18, 22, 42, 24, 25, 26, 48));
        result.putAll(11, ImmutableSet.of(33, 1, 0, 2, 12, 4, 16, 5, 6, 21, 38, 39, 42, 10, 45, 26));
        result.putAll(12, ImmutableSet.of(2, 30, 4, 16, 35, 42, 5, 18, 26, 39));
        result.putAll(13, ImmutableSet.of(33, 16));
        result.putAll(14, ImmutableSet.of(33, 4, 16, 23, 35, 42, 17, 18, 36, 47, 39));
        result.putAll(15, ImmutableSet.of(33, 2, 4, 16, 35, 27, 5, 17, 18, 6, 21, 36, 39, 41, 23, 42, 10, 44, 25, 45, 47, 26, 48));
        result.putAll(16, ImmutableSet.of(1, 3, 2, 0, 4, 27, 5, 6, 7, 28, 9, 30, 10, 33, 12, 14, 15, 34, 35, 17, 18, 19, 21, 36, 37, 38, 39, 23, 42, 43, 24, 25, 45, 47, 48, 26));
        result.putAll(17, ImmutableSet.of(33, 3, 1, 4, 14, 16, 15, 27, 35, 5, 18, 7, 6, 36, 9, 39, 30, 41, 23, 42, 10, 47, 26, 48));
        result.putAll(18, ImmutableSet.of(1, 2, 3, 4, 27, 5, 6, 7, 10, 33, 12, 13, 16, 34, 35, 17, 21, 36, 39, 42, 24, 25, 45, 47, 26));
        result.putAll(19, ImmutableSet.of(1, 4, 42, 10, 37, 26));
        result.putAll(21, ImmutableSet.of(13, 16, 42));
        result.putAll(22, ImmutableSet.of(16, 10, 26));
        result.putAll(23, ImmutableSet.of(16, 24));
        result.putAll(24, ImmutableSet.of(16, 42, 10, 26));
        result.putAll(25, ImmutableSet.of(33, 2, 3, 16, 35, 5, 18, 21, 36, 39, 42, 10, 47, 26));
        result.putAll(26, ImmutableSet.of(33, 2, 1, 4, 16, 15, 35, 5, 17, 8, 6, 18, 19, 37, 38, 39, 30, 42, 10, 43, 25, 45, 47, 48));
        result.putAll(27, ImmutableSet.of(1, 4, 16, 35, 42, 10, 18, 26));
        result.putAll(28, ImmutableSet.of(33, 40, 16, 35, 42, 17, 10, 6, 18, 39, 26));
        result.putAll(30, ImmutableSet.of(12, 4, 16, 35, 42, 5, 18, 45, 39, 26));
        result.putAll(32, ImmutableSet.of(42));
        result.putAll(33, ImmutableSet.of(13, 2, 0, 3, 4, 15, 16, 27, 5, 17, 18, 28, 36, 39, 42, 10, 44, 25, 26, 48));
        result.putAll(34, ImmutableSet.of(42, 39));
        result.putAll(35, ImmutableSet.of(2, 1, 4, 16, 5, 18, 9, 38, 39, 42, 10, 24, 45, 47, 26));
        result.putAll(36, ImmutableSet.of(33, 16, 15, 42, 39, 26));
        result.putAll(37, ImmutableSet.of(42, 10, 19, 48, 26));
        result.putAll(38, ImmutableSet.of(1, 47));
        result.putAll(39, ImmutableSet.of(33, 12, 3, 16, 35, 5, 7, 18, 8, 30, 42, 10, 24, 25, 47, 26));
        result.putAll(40, ImmutableSet.of(16, 42, 17));
        result.putAll(41, ImmutableSet.of(42, 39));
        result.putAll(42, ImmutableSet.of(3, 1, 4, 27, 5, 7, 6, 28, 30, 10, 33, 13, 12, 16, 34, 15, 35, 17, 18, 21, 36, 38, 39, 41, 23, 24, 43, 44, 25, 45, 47, 26, 48));
        result.putAll(43, ImmutableSet.of(4, 16, 5, 42, 10));
        result.putAll(44, ImmutableSet.of(3, 1, 16, 27, 35, 42, 10, 18, 7, 48, 39));
        result.putAll(45, ImmutableSet.of(12, 2, 0, 4, 16, 35, 5, 7, 6, 18, 9, 37, 38, 39, 30, 23, 42, 10, 24, 25, 26));
        result.putAll(46, ImmutableSet.of(45));
        result.putAll(47, ImmutableSet.of(2, 12, 4, 16, 35, 17, 18, 38, 39, 30, 42, 10, 24, 25, 26));
        result.putAll(48, ImmutableSet.of(1, 3, 2, 12, 4, 16, 34, 27, 35, 5, 18, 7, 37, 39, 29, 30, 42, 10, 44, 25, 45, 47, 26));
        return result;
    }

    private static final Graph<Integer, Object> graph = createGraphFrom(edgeTargetsByOrigin);

    private static Graph<Integer, Object> createGraphFrom(Multimap<Integer, Integer> edges) {
        Graph<Integer, Object> result = new Graph<>();
        addNodes(result, edges);
        addEdges(result, edges);
        return result;
    }

    private static void addNodes(Graph<Integer, Object> result, Multimap<Integer, Integer> edges) {
        Set<Integer> nodes = new HashSet<>();
        nodes.addAll(edges.keySet());
        nodes.addAll(edges.values());
        result.addNodes(nodes);
    }

    private static void addEdges(Graph<Integer, Object> result, Multimap<Integer, Integer> targetNodesByOriginNodes) {
        List<Edge<Integer, Object>> edges = new ArrayList<>();
        for (Map.Entry<Integer, Collection<Integer>> targetNodesByOrigin : targetNodesByOriginNodes.asMap().entrySet()) {
            for (Integer target : targetNodesByOrigin.getValue()) {
                edges.add(newEdge(targetNodesByOrigin.getKey(), target));
            }
        }
        result.addEdges(edges);
    }

    public static Graph<Integer, Object> get() {
        return graph;
    }
}
