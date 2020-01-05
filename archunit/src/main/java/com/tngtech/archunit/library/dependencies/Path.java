/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.library.dependencies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import static com.google.common.collect.Iterables.getLast;

class Path<T, ATTACHMENT> {
    private final List<Edge<T, ATTACHMENT>> edges;

    Path() {
        this(Collections.<Edge<T, ATTACHMENT>>emptyList());
    }

    Path(Path<T, ATTACHMENT> other) {
        this(other.getEdges());
    }

    Path(List<Edge<T, ATTACHMENT>> edges) {
        this.edges = new ArrayList<>(edges);
        validateEdgesConnect();
    }

    private void validateEdgesConnect() {
        if (edges.isEmpty()) {
            return;
        }
        Object expectedFrom = edges.get(0).getFrom();
        for (Edge<T, ?> edge : edges) {
            verifyEdgeFromMatches(expectedFrom, edge);
            expectedFrom = edge.getTo();
        }
    }

    private void verifyEdgeFromMatches(Object expectedFrom, Edge<T, ?> edge) {
        if (!expectedFrom.equals(edge.getFrom())) {
            throw new IllegalArgumentException("Edges are not connected: " + edges);
        }
    }

    List<Edge<T, ATTACHMENT>> getEdges() {
        return ImmutableList.copyOf(edges);
    }

    Set<Edge<T, ATTACHMENT>> getSetOfEdges() {
        return ImmutableSet.copyOf(edges);
    }

    Path<T, ATTACHMENT> append(Edge<T, ATTACHMENT> edge) {
        if (!edges.isEmpty()) {
            verifyEdgeFromMatches(getLast(edges).getTo(), edge);
        }
        edges.add(edge);
        return this;
    }

    boolean isEmpty() {
        return edges.isEmpty();
    }

    T getStart() {
        if (edges.isEmpty()) {
            throw new NoSuchElementException("Empty path has no start");
        }
        return edges.get(0).getFrom();
    }

    T getEnd() {
        if (edges.isEmpty()) {
            throw new NoSuchElementException("Empty path has no end");
        }
        return edges.get(edges.size() - 1).getTo();
    }

    public boolean isCycle() {
        return !isEmpty() && getStart().equals(getEnd());
    }

    @Override
    public int hashCode() {
        return Objects.hash(edges);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Path<?, ?> other = (Path<?, ?>) obj;
        return Objects.equals(this.edges, other.edges);
    }

    @Override
    public String toString() {
        return "Path{" + edgesToString() + '}';
    }

    String edgesToString() {
        return "edges=" + edges;
    }
}
