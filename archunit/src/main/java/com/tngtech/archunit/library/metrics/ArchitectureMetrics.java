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

import com.google.common.collect.FluentIterable;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.base.Predicate;
import com.tngtech.archunit.core.domain.JavaClass;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.Guava.toGuava;
import static com.tngtech.archunit.core.domain.Dependency.Functions.GET_TARGET_CLASS;
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.domain.properties.HasModifiers.Predicates.modifier;

@PublicAPI(usage = ACCESS)
public final class ArchitectureMetrics {
    private ArchitectureMetrics() {
    }

    /**
     * Calculates system architecture metrics as defined by John Lakos.
     * This method is a specific version of {@link #lakosMetrics(MetricsComponents, Function)} for {@link JavaClass JavaClasses}.
     *
     * @param components The components to calculate the metrics for
     * @return The calculated {@link LakosMetrics}
     */
    @PublicAPI(usage = ACCESS)
    public static LakosMetrics lakosMetrics(MetricsComponents<JavaClass> components) {
        return lakosMetrics(components, GET_JAVA_CLASS_DEPENDENCIES);
    }

    /**
     * Calculates system architecture metrics as defined by John Lakos.
     *
     * @param components The components to calculate the metrics for
     * @param getDependencies A function to derive for each element of a component the dependencies to all other elements
     * @param <T> The type of the elements
     * @return The calculated {@link LakosMetrics}
     */
    @PublicAPI(usage = ACCESS)
    public static <T> LakosMetrics lakosMetrics(MetricsComponents<T> components, Function<T, Collection<T>> getDependencies) {
        return new LakosMetrics(components, getDependencies);
    }

    /**
     * Calculates system component dependency metrics as defined by Robert C. Martin.
     *
     * @param components The components to calculate the metrics for
     * @return The calculated {@link ComponentDependencyMetrics}
     */
    @PublicAPI(usage = ACCESS)
    public static ComponentDependencyMetrics componentDependencyMetrics(MetricsComponents<JavaClass> components) {
        return new ComponentDependencyMetrics(components, GET_JAVA_CLASS_DEPENDENCIES);
    }

    /**
     * Calculates system component visibility metrics as defined by Herbert Dowalil.
     * This method is a specific version of {@link #visibilityMetrics(MetricsComponents, Predicate)} where the
     * elements can only be of type {@link JavaClass} and a class is considered visible, if and only if it is public.
     *
     * @param components The components to calculate the metrics for
     * @return The calculated {@link VisibilityMetrics}
     */
    @PublicAPI(usage = ACCESS)
    public static VisibilityMetrics visibilityMetrics(MetricsComponents<JavaClass> components) {
        return visibilityMetrics(components, modifier(PUBLIC));
    }

    /**
     * Calculates system component visibility metrics as defined by Herbert Dowalil.
     *
     * @param components The components to calculate the metrics for
     * @return The calculated {@link VisibilityMetrics}
     */
    @PublicAPI(usage = ACCESS)
    public static <T> VisibilityMetrics visibilityMetrics(MetricsComponents<T> components, Predicate<? super T> isVisible) {
        return new VisibilityMetrics(components, isVisible);
    }

    private static final Function<JavaClass, Collection<JavaClass>> GET_JAVA_CLASS_DEPENDENCIES = new Function<JavaClass, Collection<JavaClass>>() {
        @Override
        public Collection<JavaClass> apply(JavaClass javaClass) {
            return FluentIterable.from(javaClass.getDirectDependenciesFromSelf()).transform(toGuava(GET_TARGET_CLASS)).toSet();
        }
    };
}
