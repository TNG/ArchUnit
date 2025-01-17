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

import java.util.List;
import java.util.Objects;

class CycleInternal<EDGE extends Edge<?>> implements Cycle<EDGE> {
    private final Path<EDGE> path;

    CycleInternal(List<EDGE> edges) {
        this(new Path<>(edges));
    }

    CycleInternal(Path<EDGE> path) {
        if (!path.formsCycle()) {
            throwNoCycleException(path);
        }
        this.path = path;
    }

    private void throwNoCycleException(Path<EDGE> path) {
        throw new IllegalArgumentException("The supplied edges do not form a cycle. Edges were " + path);
    }

    @Override
    public List<EDGE> getEdges() {
        return path.getEdges();
    }

    @Override
    public int hashCode() {
        return Objects.hash(path.getEdges());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CycleInternal<?> other = (CycleInternal<?>) obj;
        return Objects.equals(this.path.getEdges(), other.path.getEdges());
    }

    @Override
    public String toString() {
        return "Cycle{" + path.edgesToString() + '}';
    }
}
