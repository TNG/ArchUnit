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
package com.tngtech.archunit.library.metrics;

import java.util.Collection;
import java.util.Map;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ForwardingCollection;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaPackage;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * A collection of {@link MetricsComponent}. Provides factory methods create components from
 * ArchUnit domain objects like {@link JavaPackage} or to componentize sets of elements.
 *
 * @see #of(Collection)
 * @see #from(Collection, Function)
 * @see #fromPackages(Collection)
 * @see #fromClasses(Collection)
 */
@PublicAPI(usage = ACCESS)
public final class MetricsComponents<T> extends ForwardingCollection<MetricsComponent<T>> {
    private final Map<String, MetricsComponent<T>> componentsByIdentifier;

    private MetricsComponents(Collection<MetricsComponent<T>> components) {
        this.componentsByIdentifier = Maps.uniqueIndex(components, new com.google.common.base.Function<MetricsComponent<T>, String>() {
            @Override
            public String apply(MetricsComponent<T> input) {
                return input.getIdentifier();
            }
        });
    }

    /**
     * @param identifier The identifier of a component
     * @return The component with the specified identifier if present, {@code Optional.absent()} otherwise.
     */
    @PublicAPI(usage = ACCESS)
    public Optional<MetricsComponent<T>> tryGetComponent(String identifier) {
        return Optional.ofNullable(componentsByIdentifier.get(identifier));
    }

    @Override
    protected Collection<MetricsComponent<T>> delegate() {
        return componentsByIdentifier.values();
    }

    /**
     * @see #of(Collection)
     */
    @SafeVarargs
    @PublicAPI(usage = ACCESS)
    public static <T> MetricsComponents<T> of(MetricsComponent<T>... components) {
        return of(ImmutableSet.copyOf(components));
    }

    /**
     * Creates {@link MetricsComponents} containing the supplied collection of {@link MetricsComponent}.<br>
     * Note that the identifiers of the passed components must all be unique.
     *
     * @param metricsComponents The components the result will contain
     * @param <T> The type of the elements contained within each component
     * @return The created {@link MetricsComponents}
     * @throws IllegalArgumentException if any two components have a duplicate identifier
     */
    @PublicAPI(usage = ACCESS)
    public static <T> MetricsComponents<T> of(Collection<MetricsComponent<T>> metricsComponents) {
        return new MetricsComponents<>(ImmutableSet.copyOf(metricsComponents));
    }

    /**
     * Partitions the passed set of elements into components according to the specified {@code identifierFunction}.
     * That is for each element of {@code elements} the respective component identifier will be derived via the passed
     * {@code identifierFunction}. For each such identifier a component will be created and all elements with
     * the same identifier will end up in the same component.
     *
     * @param elements The elements to componentize
     * @param identifierFunction A function to create the respective identifier each element belongs to
     * @param <T> The type of the elements
     * @return {@link MetricsComponents} containing one component for each unique identifier created this way
     */
    @PublicAPI(usage = ACCESS)
    public static <T> MetricsComponents<T> from(Collection<T> elements, Function<? super T, String> identifierFunction) {
        SetMultimap<String, T> partitioned = HashMultimap.create();
        for (T element : elements) {
            partitioned.put(identifierFunction.apply(element), element);
        }
        ImmutableSet.Builder<MetricsComponent<T>> components = ImmutableSet.builder();
        for (Map.Entry<String, Collection<T>> entry : partitioned.asMap().entrySet()) {
            components.add(MetricsComponent.of(entry.getKey(), entry.getValue()));
        }
        return MetricsComponents.of(components.build());
    }

    /**
     * Creates one {@link MetricsComponent} for each passed {@link JavaPackage} containing
     * all classes contained in the respective package or any subpackage. The {@link MetricsComponent#getIdentifier() identifier}
     * for each component will be the full package name of the respective {@link JavaPackage}.
     *
     * @param packages A collection of {@link JavaPackage packages} to derive the components from
     * @return {@link MetricsComponents} where components mirror the passed packages
     */
    @PublicAPI(usage = ACCESS)
    public static MetricsComponents<JavaClass> fromPackages(Collection<JavaPackage> packages) {
        ImmutableSet.Builder<MetricsComponent<JavaClass>> components = ImmutableSet.builder();
        for (JavaPackage javaPackage : packages) {
            components.add(MetricsComponent.of(javaPackage.getName(), javaPackage.getAllClasses()));
        }
        return MetricsComponents.of(components.build());
    }

    /**
     * Creates one {@link MetricsComponent} for each passed {@link JavaClass} containing only
     * this class itself. The {@link MetricsComponent#getIdentifier() identifier}
     * for each component will be the fully qualified class name of the contained {@link JavaClass}.
     *
     * @param classes The {@link JavaClass classes} to created components from
     * @return {@link MetricsComponents} where components mirror the passed classes one to one
     */
    @PublicAPI(usage = ACCESS)
    public static MetricsComponents<JavaClass> fromClasses(Collection<JavaClass> classes) {
        ImmutableSet.Builder<MetricsComponent<JavaClass>> components = ImmutableSet.builder();
        for (JavaClass javaClass : classes) {
            components.add(MetricsComponent.of(javaClass.getName(), javaClass));
        }
        return MetricsComponents.of(components.build());
    }
}
