package com.tngtech.archunit.library.dependencies;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ContiguousSet;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Range;
import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.library.dependencies.Graph.Cycles;
import org.junit.Test;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.DiscreteDomain.integers;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Sets.cartesianProduct;
import static com.tngtech.archunit.library.dependencies.CycleConfiguration.MAX_NUMBER_OF_CYCLES_TO_DETECT_PROPERTY_NAME;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class GraphTest {
    private static final Random random = new Random();

    @Test
    public void graph_without_cycles() {
        Graph<String, String> graph = new Graph<>();

        graph.addNodes(asList(randomNode(), randomNode(), randomNode()));

        assertThat(graph.findCycles()).isEmpty();
    }

    @Test
    public void three_node_cycle_is_detected() {
        Graph<String, String> graph = new Graph<>();

        String nodeA = "Node-A";
        String nodeB = "Node-B";
        String nodeC = "Node-C";
        graph.addNodes(asList(nodeA, nodeB, nodeC));
        graph.addEdges(ImmutableSet.of(stringEdge(nodeA, nodeB), stringEdge(nodeB, nodeC), stringEdge(nodeC, nodeA)));

        Cycle<String, String> cycle = getOnlyElement(graph.findCycles());

        assertThat(cycle.getEdges()).hasSize(3);
        assertEdgeExists(cycle, nodeA, nodeB);
        assertEdgeExists(cycle, nodeB, nodeC);
        assertEdgeExists(cycle, nodeC, nodeA);
    }

    @Test
    public void sub_cycle_of_three_node_graph_is_detected() {
        Graph<String, String> graph = new Graph<>();

        String nodeA = "Node-A";
        String nodeB = "Node-B";
        String nodeC = "Node-C";
        graph.addNodes(asList(nodeA, nodeB, nodeC));
        graph.addEdges(ImmutableSet.of(stringEdge(nodeB, nodeA), stringEdge(nodeA, nodeB)));

        Cycle<String, String> cycle = getOnlyElement(graph.findCycles());
        assertThat(cycle.getEdges()).hasSize(2);
        assertEdgeExists(cycle, nodeA, nodeB);
        assertEdgeExists(cycle, nodeB, nodeA);
    }

    @Test
    public void nested_cycles_are_detected() {
        Graph<String, String> graph = new Graph<>();

        String nodeA = "Node-A";
        String nodeB = "Node-B";
        String nodeC = "Node-C";
        graph.addNodes(asList(nodeA, nodeB, nodeC));
        graph.addEdges(ImmutableSet.of(stringEdge(nodeB, nodeA), stringEdge(nodeA, nodeB), stringEdge(nodeC, nodeA), stringEdge(nodeB, nodeC)));

        assertThat(graph.findCycles()).hasSize(2);
    }

    @Test
    public void multiple_cycles_are_detected() {
        Graph<String, String> graph = new Graph<>();

        Cycle<String, String> threeElements = randomCycle(3);
        Cycle<String, String> fourElements = randomCycle(4);
        Cycle<String, String> fiveElements = randomCycle(5);

        addCycles(graph, threeElements, fourElements, fiveElements);
        addCrossLink(graph, threeElements, fourElements);
        addCrossLink(graph, fourElements, fiveElements);

        Collection<Cycle<String, String>> cycles = graph.findCycles();

        assertThat(cycles).containsOnly(threeElements, fourElements, fiveElements);
    }

    @Test
    public void double_linked_three_node_cycle_results_in_five_cycles() {
        Graph<String, String> graph = new Graph<>();

        Cycle<String, String> threeElements = randomCycle(3);

        addCycles(graph, threeElements);
        for (Edge<String, String> edge : threeElements.getEdges()) {
            graph.addEdges(singleEdge(edge.getTo(), edge.getFrom()));
        }

        assertThat(graph.findCycles()).hasSize(5);
    }

    @Test
    public void complete_graph() {
        Graph<Integer, Integer> completeGraph = createCompleteGraph(3);
        Iterable<Cycle<Integer, Integer>> cycles = completeGraph.findCycles();

        assertThat(cycles).containsOnly(createCycle(ImmutableList.of(0, 1, 2, 0)),
                createCycle(ImmutableList.of(0, 2, 1, 0)),
                createCycle(ImmutableList.of(0, 1, 0)),
                createCycle(ImmutableList.of(1, 2, 1)),
                createCycle(ImmutableList.of(2, 0, 2)));
    }

    @Test
    public void graph_which_causes_error_when_dependently_blocked_nodes_are_not_cleared_after_unblocking() {
        ImmutableSet<Integer> nodes = ImmutableSet.of(0, 1, 2, 3, 4, 5);
        Graph<Integer, Object> graph = new Graph<>();
        graph.addNodes(nodes);

        graph.addEdges(ImmutableSet.of(
                newEdge(0, 4),
                newEdge(1, 0),
                newEdge(1, 5),
                newEdge(2, 1),
                newEdge(3, 1),
                newEdge(3, 5),
                newEdge(4, 3),
                newEdge(5, 1),
                newEdge(5, 2)
        ));

        assertThat(graph.findCycles()).isNotEmpty();
    }

    // This test covers some edge cases, e.g. if too many nodes stay blocked
    @Test
    public void finds_cycles_in_real_life_graph() {
        Graph<Integer, Object> graph = RealLifeGraph.get();
        int expectedNumberOfCycles = 10000;
        ArchConfiguration.get().setProperty(MAX_NUMBER_OF_CYCLES_TO_DETECT_PROPERTY_NAME, String.valueOf(expectedNumberOfCycles));

        Cycles<Integer, Object> cycles = graph.findCycles();

        assertThat(cycles).hasSize(expectedNumberOfCycles);
        assertThat(cycles.maxNumberOfCyclesReached()).as("maximum number of cycles reached").isTrue();
    }

    @SuppressWarnings("unchecked")
    private Graph<Integer, Integer> createCompleteGraph(int n) {
        ContiguousSet<Integer> integers = ContiguousSet.create(Range.closedOpen(0, n), integers());
        Graph<Integer, Integer> graph = new Graph<>();
        graph.addNodes(integers);
        graph.addEdges(FluentIterable.from(cartesianProduct(integers, integers))
                .filter(new Predicate<List<Integer>>() {
                    @Override
                    public boolean apply(List<Integer> input) {
                        return !input.get(0).equals(input.get(1));
                    }
                })
                .transform(new Function<List<Integer>, Edge<Integer, Integer>>() {
                    @Override
                    public Edge<Integer, Integer> apply(List<Integer> input) {
                        return integerEdge(input.get(0), input.get(1));
                    }
                }).toSet());
        return graph;
    }

    private Cycle<Integer, Integer> createCycle(List<Integer> numbers) {
        ImmutableList.Builder<Edge<Integer, Integer>> builder = ImmutableList.builder();
        for (int i = 0; i < numbers.size() - 1; i++) {
            builder.add(integerEdge(numbers.get(i), numbers.get(i + 1)));
        }
        return new Cycle<>(builder.build());
    }

    private Cycle<String, String> randomCycle(int numberOfNodes) {
        checkArgument(numberOfNodes > 1, "A cycle can't be formed by less than 2 nodes");
        Path<String, String> path = new Path<>(singleEdgeList(randomNode(), randomNode()));
        for (int i = 0; i < numberOfNodes - 2; i++) {
            path.append(stringEdge(path.getEnd(), randomNode()));
        }
        return new Cycle<>(path.append(stringEdge(path.getEnd(), path.getStart())));
    }

    @SafeVarargs
    private final void addCycles(Graph<String, String> graph, Cycle<String, String>... cycles) {
        for (Cycle<String, String> cycle : cycles) {
            for (Edge<String, String> edge : cycle.getEdges()) {
                graph.addNodes(asList(edge.getFrom(), edge.getTo()));
            }
            graph.addEdges(cycle.getEdges());
        }
    }

    private void addCrossLink(Graph<String, String> graph, Cycle<String, String> first, Cycle<String, String> second) {
        Random rand = new Random();
        String origin = first.getEdges().get(rand.nextInt(first.getEdges().size())).getFrom();
        String target = second.getEdges().get(rand.nextInt(second.getEdges().size())).getFrom();
        graph.addEdges(singleEdge(origin, target));
    }

    private static void assertEdgeExists(Cycle<?, ?> cycle, Object from, Object to) {
        for (Edge<?, ?> edge : cycle.getEdges()) {
            if (edge.getFrom().equals(from) && edge.getTo().equals(to)) {
                return;
            }
        }
        throw new AssertionError("Expected Cycle to contain an edge from " + from + " to " + to);
    }

    static String randomNode() {
        return "" + random.nextLong() + System.nanoTime();
    }

    static Edge<String, String> stringEdge(String nodeA, String nodeB) {
        return newEdge(nodeA, nodeB);
    }

    private Edge<Integer, Integer> integerEdge(Integer origin, Integer target) {
        return newEdge(origin, target);
    }

    static List<Edge<String, String>> singleEdgeList(String from, String to) {
        return singletonList(stringEdge(from, to));
    }

    static Set<Edge<String, String>> singleEdge(String from, String to) {
        return singleton(stringEdge(from, to));
    }

    static <NODE, ATTACHMENT> Edge<NODE, ATTACHMENT> newEdge(NODE from, NODE to) {
        return new Edge<>(from, to, Collections.<ATTACHMENT>emptySet());
    }
}
