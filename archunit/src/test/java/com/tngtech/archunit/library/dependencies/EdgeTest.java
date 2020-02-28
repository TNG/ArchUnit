package com.tngtech.archunit.library.dependencies;

import java.util.Collections;

import org.junit.Test;

import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

public class EdgeTest {
    private final Object from = new Object();
    private final Object to = new Object();

    @Test
    public void edges_are_equal_iff_from_and_to_are_equal() {

        assertThat(new Edge<>(from, to, emptySet())).isEqualTo(new Edge<>(from, to, emptySet()));
        assertThat(new Edge<>(from, to, emptySet())).isNotEqualTo(new Edge<>(new Object(), to, emptySet()));
        assertThat(new Edge<>(from, to, emptySet())).isNotEqualTo(new Edge<>(from, new Object(), emptySet()));

        Edge<Object, Object> equalWithAttachment = new Edge<>(from, to, singletonList(new Object()));
        assertThat(new Edge<>(from, to, emptySet())).isEqualTo(equalWithAttachment);
    }

    @Test
    public void hashCode_of_two_equal_edges_is_equal() {
        Edge<Object, Object> equalEdge = new Edge<>(from, to, singletonList(new Object()));
        assertThat(new Edge<>(from, to, emptySet()).hashCode()).isEqualTo(equalEdge.hashCode());
    }

}
