package com.tngtech.archunit.library.dependencies;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Test;

import static com.tngtech.archunit.library.dependencies.GraphTest.randomNode;
import static org.assertj.core.api.Assertions.assertThat;

public class CycleTest extends PathTest {
    @Test
    public void rejects_single_edge() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("do not form a cycle");

        new Cycle<>(Lists.<Edge<String, String>>newArrayList(new SimpleEdge(randomNode(), randomNode())));
    }

    @Test
    public void minimal_nontrivial_cycle() {
        String nodeA = "Node-A";
        String nodeB = "Node-B";
        Cycle<String, ?> cycle = new Cycle<>(Lists.<Edge<String, String>>newArrayList(new SimpleEdge(nodeA, nodeB), new SimpleEdge(nodeB, nodeA)));

        assertThat(cycle.getEdges()).hasSize(2);
    }

    @Override
    protected void newPath(List<Edge<String, String>> edges) {
        new Cycle<>(edges);
    }
}