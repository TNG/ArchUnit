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

import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;

import static com.google.common.base.Preconditions.checkNotNull;

class Cycle<T, ATTACHMENT> {
    private final Path<T, ATTACHMENT> path;

    Cycle(List<Edge<T, ATTACHMENT>> edges) {
        this(new Path<>(ImmutableList.copyOf(edges)));
    }

    Cycle(Path<T, ATTACHMENT> path) {
        this.path = checkNotNull(path);
        validate(path);
    }

    List<Edge<T, ATTACHMENT>> getEdges() {
        return path.getEdges();
    }

    private void validate(Path<T, ATTACHMENT> path) {
        if (path.isEmpty()) {
            throwNoCycleException(path);
        }
        validateStartEqualsEnd(path);
    }

    private void validateStartEqualsEnd(Path<T, ATTACHMENT> path) {
        T edgeStart = path.getStart();
        T edgeEnd = path.getEnd();
        if (!edgeEnd.equals(edgeStart)) {
            throwNoCycleException(path);
        }
    }

    private void throwNoCycleException(Path<T, ATTACHMENT> path) {
        throw new IllegalArgumentException("The supplied edges do not form a cycle. Edges were " + path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path.getSetOfEdges());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Cycle<?, ?> other = (Cycle<?, ?>) obj;
        return Objects.equals(this.path.getSetOfEdges(), other.path.getSetOfEdges());
    }

    @Override
    public String toString() {
        return "Cycle{" + path.edgesToString() + '}';
    }

    public static <T, ATTACHMENT> Cycle<T, ATTACHMENT> from(Path<T, ATTACHMENT> path) {
        return new Cycle<>(path);
    }
}
