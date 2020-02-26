package com.tngtech.archunit.library.dependencies;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.tngtech.archunit.library.dependencies.GraphTest.randomNode;
import static com.tngtech.archunit.library.dependencies.GraphTest.stringEdge;
import static java.util.Arrays.asList;

public class PathTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void rejects_invalid_edges() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Edges are not connected");

        new Path<>(asList(stringEdge(randomNode(), randomNode()), stringEdge(randomNode(), randomNode())));
    }
}