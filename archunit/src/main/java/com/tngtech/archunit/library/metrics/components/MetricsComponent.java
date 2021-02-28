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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Function;
import com.google.common.collect.ForwardingSet;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.tngtech.archunit.base.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.singleton;

public class MetricsComponent<T> extends ForwardingSet<T> {
    private final String identifier;
    private final Set<T> content;
    private final Set<MetricsElementDependency<T>> elementDependenciesFromSelf;
    private Map<String, MetricsComponentDependency<T>> componentDependenciesFromSelfByTarget;
    private Map<String, MetricsComponentDependency<T>> componentDependenciesToSelfByOrigin;

    private MetricsComponent(String identifier, Set<T> content, Function<T, Set<MetricsElementDependency<T>>> getElementDependencies) {
        this.identifier = checkNotNull(identifier);
        this.content = checkNotNull(content);
        ImmutableSet.Builder<MetricsElementDependency<T>> elementDependenciesBuilder = ImmutableSet.builder();
        for (T element : content) {
            elementDependenciesBuilder.addAll(getElementDependencies.apply(element));
        }
        elementDependenciesFromSelf = elementDependenciesBuilder.build();
    }

    public String getIdentifier() {
        return identifier;
    }

    public String getName() {
        return identifier;
    }

    public Set<MetricsElementDependency<T>> getElementDependenciesTo(MetricsComponent<T> component) {
        MetricsComponentDependency<T> dependency = componentDependenciesFromSelfByTarget.get(component.getIdentifier());
        return dependency != null ? dependency : Collections.<MetricsElementDependency<T>>emptySet();
    }

    private Optional<MetricsComponentDependency<T>> getComponentDependencyTo(MetricsComponent<T> component) {
        return Optional.fromNullable(componentDependenciesFromSelfByTarget.get(component.getIdentifier()));
    }

    public Set<MetricsComponentDependency<T>> getComponentDependenciesFromSelf() {
        return ImmutableSet.copyOf(componentDependenciesFromSelfByTarget.values());
    }

    public Set<MetricsComponentDependency<T>> getComponentDependenciesToSelf() {
        return ImmutableSet.copyOf(componentDependenciesToSelfByOrigin.values());
    }

    @Override
    protected Set<T> delegate() {
        return content;
    }

    void finishComponentDependenciesFromSelf(Set<MetricsComponent<T>> allComponents) {
        ImmutableMap.Builder<String, MetricsComponentDependency<T>> componentDependenciesBuilder = ImmutableMap.builder();
        for (MetricsComponent<T> other : Sets.difference(allComponents, singleton(this))) {
            ImmutableSet.Builder<MetricsElementDependency<T>> dependenciesToOtherBuilder = ImmutableSet.builder();
            for (MetricsElementDependency<T> elementDependency : elementDependenciesFromSelf) {
                if (other.contains(elementDependency.getTarget())) {
                    dependenciesToOtherBuilder.add(elementDependency);
                }
            }
            ImmutableSet<MetricsElementDependency<T>> dependenciesToOther = dependenciesToOtherBuilder.build();
            if (!dependenciesToOther.isEmpty()) {
                MetricsComponentDependency<T> dependency = MetricsComponentDependency.of(this, other, dependenciesToOther);
                componentDependenciesBuilder.put(other.getIdentifier(), dependency);
            }
        }
        componentDependenciesFromSelfByTarget = componentDependenciesBuilder.build();
    }

    void finishComponentDependenciesToSelf(Set<MetricsComponent<T>> allComponents) {
        ImmutableMap.Builder<String, MetricsComponentDependency<T>> componentDependenciesBuilder = ImmutableMap.builder();
        for (MetricsComponent<T> other : Sets.difference(allComponents, singleton(this))) {
            Optional<MetricsComponentDependency<T>> dependencyToSelf = other.getComponentDependencyTo(this);
            if (dependencyToSelf.isPresent()) {
                componentDependenciesBuilder.put(other.getIdentifier(), dependencyToSelf.get());
            }
        }
        componentDependenciesToSelfByOrigin = componentDependenciesBuilder.build();
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("identifier", identifier)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MetricsComponent)) {
            return false;
        }
        MetricsComponent<?> that = (MetricsComponent<?>) o;
        return Objects.equals(identifier, that.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }

    public static <T> MetricsComponent<T> of(String identifier, Set<T> content, Function<T, Set<MetricsElementDependency<T>>> getElementDependencies) {
        return new MetricsComponent<>(identifier, content, getElementDependencies);
    }
}
