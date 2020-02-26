package com.tngtech.archunit.library.dependencies;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.tngtech.archunit.library.dependencies.GraphTest.randomNode;
import static com.tngtech.archunit.library.dependencies.GraphTest.stringEdge;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class CycleTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void rejects_invalid_edges() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Edges are not connected");

        new Cycle<>(asList(stringEdge(randomNode(), randomNode()), stringEdge(randomNode(), randomNode())));
    }

    @Test
    public void rejects_single_edge() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("do not form a cycle");

        new Cycle<>(singletonList(stringEdge(randomNode(), randomNode())));
    }

    @Test
    public void minimal_nontrivial_cycle() {
        String nodeA = "Node-A";
        String nodeB = "Node-B";
        Cycle<String, ?> cycle = new Cycle<>(asList(stringEdge(nodeA, nodeB), stringEdge(nodeB, nodeA)));

        assertThat(cycle.getEdges()).hasSize(2);
    }
}