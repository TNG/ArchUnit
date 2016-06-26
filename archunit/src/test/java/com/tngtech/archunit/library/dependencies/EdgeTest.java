package com.tngtech.archunit.library.dependencies;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class EdgeTest {
    private final Object from = new Object();
    private final Object to = new Object();

    @Test
    public void edges_are_equal_iff_from_and_to_are_equal() {

        assertThat(new Edge<>(from, to)).isEqualTo(new Edge<>(from, to));
        assertThat(new Edge<>(from, to)).isNotEqualTo(new Edge<>(new Object(), to));
        assertThat(new Edge<>(from, to)).isNotEqualTo(new Edge<>(from, new Object()));

        Edge<Object, Object> equalWithAttachment = new Edge<>(from, to);
        equalWithAttachment.addAttachment(new Object());
        assertThat(new Edge<>(from, to)).isEqualTo(equalWithAttachment);
    }

    @Test
    public void hashCode_of_two_equal_edges_is_equal() {
        Edge<Object, Object> equalEdge = new Edge<>(from, to);
        equalEdge.addAttachment(new Object());
        assertThat(new Edge<>(from, to).hashCode()).isEqualTo(equalEdge.hashCode());
    }
}