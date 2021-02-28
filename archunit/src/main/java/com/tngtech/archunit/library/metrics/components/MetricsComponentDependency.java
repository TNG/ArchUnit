/*
 * Copyright 2014-2021 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.library.metrics.components;

import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.ForwardingSet;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

public class MetricsComponentDependency<T> extends ForwardingSet<MetricsElementDependency<T>> {
    private final MetricsComponent<T> origin;
    private final MetricsComponent<T> target;
    private final Set<MetricsElementDependency<T>> elementDependencies;

    private MetricsComponentDependency(MetricsComponent<T> origin, MetricsComponent<T> target, Set<MetricsElementDependency<T>> elementDependencies) {
        for (MetricsElementDependency<T> dependency : elementDependencies) {
            if (!origin.contains(dependency.getOrigin()) || !target.contains(dependency.getTarget())) {
                throw new IllegalArgumentException(String.format("Illegal element dependencies for component dependency from %s to %s: %s", origin, target, elementDependencies));
            }
        }

        this.origin = checkNotNull(origin);
        this.target = checkNotNull(target);
        this.elementDependencies = elementDependencies;
    }

    public MetricsComponent<T> getOrigin() {
        return origin;
    }

    public MetricsComponent<T> getTarget() {
        return target;
    }

    @Override
    protected Set<MetricsElementDependency<T>> delegate() {
        return elementDependencies;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("origin", origin)
                .add("target", target)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MetricsComponentDependency)) {
            return false;
        }
        MetricsComponentDependency<?> that = (MetricsComponentDependency<?>) o;
        return Objects.equals(origin, that.origin) && Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, target);
    }

    public static <T> MetricsComponentDependency<T> of(MetricsComponent<T> origin, MetricsComponent<T> target, ImmutableSet<MetricsElementDependency<T>> dependencies) {
        return new MetricsComponentDependency<>(origin, target, dependencies);
    }
}
