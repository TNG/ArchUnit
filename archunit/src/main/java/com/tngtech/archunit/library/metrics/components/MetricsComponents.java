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

import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.ForwardingSet;
import com.google.common.collect.ImmutableSet;

import static com.google.common.collect.Sets.intersection;

public class MetricsComponents<T> extends ForwardingSet<MetricsComponent<T>> {
    private final ImmutableSet<MetricsComponent<T>> components;

    public MetricsComponents(ImmutableSet<MetricsComponent<T>> components) {
        validateDisjoint(components);
        this.components = components;
    }

    private void validateDisjoint(Set<MetricsComponent<T>> elements) {
        Set<MetricsComponent<T>> remaining = new HashSet<>(elements);
        for (MetricsComponent<T> first : elements) {
            remaining.remove(first);
            for (MetricsComponent<T> second : remaining) {
                if (!intersection(first, second).isEmpty()) {
                    throw new IllegalArgumentException(String.format(
                            "Components must be disjoint, but %s and %s are not", first.getName(), second.getName()));
                }
            }
        }
    }

    @Override
    protected Set<MetricsComponent<T>> delegate() {
        return components;
    }

    public static <T> MetricsComponents<T> of(Iterable<MetricsComponent<T>> components) {
        ImmutableSet<MetricsComponent<T>> metricsComponents = ImmutableSet.copyOf(components);
        for (MetricsComponent<T> component : metricsComponents) {
            component.finish(metricsComponents);
        }
        return new MetricsComponents<>(metricsComponents);
    }
}
