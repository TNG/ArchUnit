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

import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.Predicate;

import static com.google.common.base.Preconditions.checkArgument;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Calculates visibility metrics as defined by Herbert Dowalil in his book
 * "Modulare Softwarearchitektur: Nachhaltiger Entwurf durch Microservices, Modulithen und SOA 2.0".
 * <br>
 * Visibility refers to the property, if an element of a component is accessible from outside of the component.
 * An example would be a package, where public classes are visible from outside of the package, while package-private,
 * protected and private classes are not. The metrics are calculated by introducing the following definitions:
 * <ul>
 *     <li>Relative Visibility (<b>RV</b>): {@code num(visible_elements) / num(all_elements)} for each component</li>
 *     <li>Average Relative Visibility (<b>ARV</b>): The average of all {@code RV} values</li>
 *     <li>Global Relative Visibility (<b>GRV</b>): {@code num(visible_elements) / num(all_elements)} over all components</li>
 * </ul>
 * <br>
 * Consider the following example:
 * <pre><code>
 * Component1 (Visible: 2 / Invisible: 4)
 * Component2 (Visible: 3 / Invisible: 3)
 * Component3 (Visible 1 / Invisible: 9)
 * </code></pre>
 * Then<br>
 * {@code RV(Component1) = 2 / 6 = 0.33}<br>
 * {@code RV(Component2) = 3 / 6 = 0.5}<br>
 * {@code RV(Component3) = 1 / 10 = 0.1}<br>
 * {@code ARV = (0.33 + 0.5 + 0.1) / 3 = 0.31}<br>
 * {@code GRV = 6 / 22 = 0.27}.
 */
@PublicAPI(usage = ACCESS)
public final class VisibilityMetrics {
    private final ImmutableMap<String, ComponentVisibility> relativeVisibilityByComponentIdentifier;
    private final double averageRelativeVisibility;
    private final double globalRelativeVisibility;

    <T> VisibilityMetrics(MetricsComponents<T> components, Predicate<? super T> isVisible) {
        ImmutableMap.Builder<String, ComponentVisibility> relativeVisibilityByComponentIdentifierBuilder = ImmutableMap.builder();
        for (MetricsComponent<T> component : components) {
            relativeVisibilityByComponentIdentifierBuilder.put(component.getIdentifier(), new ComponentVisibility(component, isVisible));
        }
        relativeVisibilityByComponentIdentifier = relativeVisibilityByComponentIdentifierBuilder.build();
        averageRelativeVisibility = calculateAverageRelativeVisibility(relativeVisibilityByComponentIdentifier.values());
        globalRelativeVisibility = calculateGlobalRelativeVisibility(relativeVisibilityByComponentIdentifier.values());
    }

    private static double calculateAverageRelativeVisibility(Collection<ComponentVisibility> componentVisibilities) {
        double sum = 0;
        for (ComponentVisibility componentVisibility : componentVisibilities) {
            sum += componentVisibility.relativeVisibility;
        }
        return sum / componentVisibilities.size();
    }

    private static double calculateGlobalRelativeVisibility(Collection<ComponentVisibility> componentVisibilities) {
        double numberOfVisibleElements = 0;
        double numberOfAllElements = 0;
        for (ComponentVisibility componentVisibility : componentVisibilities) {
            numberOfVisibleElements += componentVisibility.numberOfVisibleElements;
            numberOfAllElements += componentVisibility.numberOfAllElements;
        }
        return numberOfVisibleElements / numberOfAllElements;
    }

    /**
     * The {@link VisibilityMetrics Relative Visibility (RV)} of the component.
     *
     * @see VisibilityMetrics
     */
    @PublicAPI(usage = ACCESS)
    public double getRelativeVisibility(String componentIdentifier) {
        checkComponentExists(componentIdentifier);
        return relativeVisibilityByComponentIdentifier.get(componentIdentifier).relativeVisibility;
    }

    /**
     * The {@link VisibilityMetrics Average Relative Visibility (ARV)} of the components.
     *
     * @see VisibilityMetrics
     */
    @PublicAPI(usage = ACCESS)
    public double getAverageRelativeVisibility() {
        return averageRelativeVisibility;
    }

    /**
     * The {@link VisibilityMetrics Global Relative Visibility (GRV)} of the components.
     *
     * @see VisibilityMetrics
     */
    @PublicAPI(usage = ACCESS)
    public double getGlobalRelativeVisibility() {
        return globalRelativeVisibility;
    }

    private void checkComponentExists(String componentIdentifier) {
        checkArgument(relativeVisibilityByComponentIdentifier.containsKey(componentIdentifier),
                "Unknown component with identifier '" + componentIdentifier + "'");
    }

    private static class ComponentVisibility {
        final int numberOfVisibleElements;
        final int numberOfAllElements;
        final double relativeVisibility;

        <T> ComponentVisibility(MetricsComponent<T> component, Predicate<? super T> isVisible) {
            int numberOfVisibleElements = 0;
            for (T element : component) {
                if (isVisible.apply(element)) {
                    numberOfVisibleElements++;
                }
            }
            this.numberOfVisibleElements = numberOfVisibleElements;
            this.numberOfAllElements = component.size();
            this.relativeVisibility = ((double) numberOfVisibleElements) / numberOfAllElements;
        }
    }
}
