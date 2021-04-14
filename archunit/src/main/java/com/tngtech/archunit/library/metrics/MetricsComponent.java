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
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ForwardingCollection;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaPackage;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Represents a component of the software system. The term "component" does not impose any wider
 * restriction than that it contains a set of elements which all share a common string identifier.<br>
 * Thus by definition two components are equal, if and only if they have the same identifier.<br>
 * The elements that a component contains are principally completely free. It could be
 * {@link JavaClass JavaClasses} or {@link JavaPackage JavaPackages}, but also any other type
 * of objects where software metrics can be calculated on. Specific metrics might then impose
 * type restrictions on the element, e.g. if metrics can only be calculated for elements that
 * specify their dependencies on other elements.
 * <br>
 * This abstraction is powerful enough to be applied to a wide variety of scenarios. For example
 * <ul>
 *     <li>Partitioning classes by their packages and using the package name as identifier would
 *         create one component per package</li>
 *     <li>Using the (unique) name of a build module and grouping all classes in that module</li>
 *     <li>Partitioning classes by their fully qualified class name would create one component
 *         per class</li>
 *     <li>Partitioning methods by their declaring class name would create components each
 *         containing exactly the methods of one specific class</li>
 * </ul>
 *
 * @param <T> The type of the elements this component contains, e.g. {@link JavaClass}
 */
@PublicAPI(usage = ACCESS)
public final class MetricsComponent<T> extends ForwardingCollection<T> {
    private final String identifier;
    private final Set<T> elements;

    private MetricsComponent(String identifier, Collection<T> elements) {
        this.identifier = checkNotNull(identifier, "identifier must not be null");
        this.elements = ImmutableSet.copyOf(elements);
    }

    /**
     * @return The (unique) identifier of this component
     * @see MetricsComponent
     */
    @PublicAPI(usage = ACCESS)
    public String getIdentifier() {
        return identifier;
    }

    /**
     * @return The elements contained in this component
     * @see MetricsComponent
     */
    @PublicAPI(usage = ACCESS)
    public Set<T> getElements() {
        return elements;
    }

    @Override
    protected Collection<T> delegate() {
        return elements;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("identifier", identifier)
                .toString();
    }

    /**
     * @see #of(String, Collection)
     */
    @SafeVarargs
    @PublicAPI(usage = ACCESS)
    public static <T> MetricsComponent<T> of(String identifier, T... elements) {
        return new MetricsComponent<>(identifier, ImmutableSet.copyOf(elements));
    }

    /**
     * @param identifier The (unique) identifier of this component
     * @param elements The elements contained inside of this component
     * @param <T> The type of the elements inside of this component
     * @return A new {@link MetricsComponent}
     */
    @PublicAPI(usage = ACCESS)
    public static <T> MetricsComponent<T> of(String identifier, Collection<T> elements) {
        return new MetricsComponent<>(identifier, elements);
    }
}
