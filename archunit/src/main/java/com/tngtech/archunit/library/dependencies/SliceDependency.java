/*
 * Copyright 2017 TNG Technology Consulting GmbH
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
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

import com.google.common.base.Joiner;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.Dependency;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public final class SliceDependency implements HasDescription {
    private final Slice origin;
    private final Slice target;

    static SliceDependency of(Slice origin, Slice target) {
        return new SliceDependency(origin, target);
    }

    private SliceDependency(Slice origin, Slice target) {
        this.origin = origin;
        this.target = target;
    }

    @PublicAPI(usage = ACCESS)
    public Slice getOrigin() {
        return origin;
    }

    @PublicAPI(usage = ACCESS)
    public Slice getTarget() {
        return target;
    }

    @Override
    public String getDescription() {
        return describe(origin, target);
    }

    private String describe(Slice slice, Slice dependencySlice) {
        return String.format("%s calls %s:%n%s",
                slice.getDescription(),
                dependencySlice.getDescription(),
                joinDependencies(slice, dependencySlice));
    }

    private String joinDependencies(Slice from, Slice to) {
        List<String> parts = new ArrayList<>();
        for (Dependency dependency : new TreeSet<>(from.getDependencies())) {
            if (to.contains(dependency.getTargetClass())) {
                parts.add(dependency.getDescription());
            }
        }
        return Joiner.on(System.lineSeparator()).join(parts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, target);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final SliceDependency other = (SliceDependency) obj;
        return Objects.equals(this.origin, other.origin)
                && Objects.equals(this.target, other.target);
    }

    @Override
    public String toString() {
        return "SliceDependency{" +
                "origin=" + origin +
                ", target=" + target +
                '}';
    }
}
