package com.tngtech.archunit.library.dependencies;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EdgeTest {
    private final Object from = new Object();
    private final Object to = new Object();

    @Test
    public void edges_are_equal_iff_from_and_to_are_equal() {

        assertThat(Edge.create(from, to)).isEqualTo(Edge.create(from, to));
        assertThat(Edge.create(from, to)).isNotEqualTo(Edge.create(new Object(), to));
        assertThat(Edge.create(from, to)).isNotEqualTo(Edge.create(from, new Object()));

        Edge<Object> equalWithAttachment = Edge.create(from, to);
        assertThat(Edge.create(from, to)).isEqualTo(equalWithAttachment);
    }

    @Test
    public void hashCode_of_two_equal_edges_is_equal() {
        Edge<Object> equalEdge = Edge.create(from, to);
        assertThat(Edge.create(from, to).hashCode()).isEqualTo(equalEdge.hashCode());
    }

}
