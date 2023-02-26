package com.tngtech.archunit.library.cycle_detection;

import java.util.List;

import org.junit.Test;

import static com.tngtech.archunit.library.cycle_detection.GraphTest.randomNode;
import static com.tngtech.archunit.library.cycle_detection.GraphTest.stringEdge;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PathTest {

    @Test
    public void rejects_invalid_edges() {
        List<Edge<String>> edges = asList(stringEdge(randomNode(), randomNode()), stringEdge(randomNode(), randomNode()));
        assertThatThrownBy(
                () -> new Path<>(edges)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Edges are not connected");
    }
}
