package com.tngtech.archunit.library.dependencies;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static com.tngtech.archunit.library.dependencies.GraphTest.randomNode;

public class PathTest {
    @Rule
    public final ExpectedException thrown = ExpectedException.none();

    @Test
    public void rejects_invalid_edges() {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Edges are not connected");

        newPath(Lists.<Edge<String, String>>newArrayList(new SimpleEdge(randomNode(), randomNode()), new SimpleEdge(randomNode(), randomNode())));
    }

    protected void newPath(List<Edge<String, String>> edges) {
        new Path<>(edges);
    }
}