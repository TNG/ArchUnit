/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.library.cycle_detection;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;

import static com.google.common.collect.Iterables.getLast;
import static java.util.Collections.emptyList;

class Path<EDGE extends Edge<?>> {
    private final List<EDGE> edges;

    Path() {
        this(emptyList());
    }

    Path(Path<EDGE> other) {
        this(other.getEdges());
    }

    Path(List<EDGE> edges) {
        this.edges = new ArrayList<>(edges);
        validateEdgesConnect();
    }

    private void validateEdgesConnect() {
        if (edges.isEmpty()) {
            return;
        }
        Object expectedFrom = edges.get(0).getOrigin();
        for (EDGE edge : edges) {
            verifyEdgeFromMatches(expectedFrom, edge);
            expectedFrom = edge.getTarget();
        }
    }

    private void verifyEdgeFromMatches(Object expectedFrom, EDGE edge) {
        if (!expectedFrom.equals(edge.getOrigin())) {
            throw new IllegalArgumentException("Edges are not connected: " + edges);
        }
    }

    List<EDGE> getEdges() {
        return ImmutableList.copyOf(edges);
    }

    boolean isEmpty() {
        return edges.isEmpty();
    }

    boolean formsCycle() {
        if (isEmpty()) {
            return false;
        }

        Object start = edges.get(0).getOrigin();
        Object end = getLast(edges).getTarget();
        return start.equals(end);
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
        Path<?> other = (Path<?>) obj;
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
