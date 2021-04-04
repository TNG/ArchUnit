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

import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.Function;
import com.tngtech.archunit.core.domain.JavaClass;

import static com.google.common.base.Preconditions.checkArgument;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT;
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;

/**
 * Calculates architecture metrics as defined by Robert C. Martin in his book
 * "Clean architecture : a craftsman's guide to software structure and design".<br>
 * <br>
 * These metrics are calculated following these definitions for each component:
 * <ul>
 *     <li>Efferent Coupling (<b>Ce</b>): The number of outgoing dependencies to any other component</li>
 *     <li>Afferent Coupling (<b>Ca</b>): The number of incoming dependencies from any other component</li>
 *     <li>Instability (<b>I</b>): {@code Ce / (Ca + Ce)}, i.e. the relationship of outgoing dependencies to all dependencies</li>
 *     <li>Abstractness (<b>A</b>): {@code num(abstract_classes) / num(all_classes)} in the component</li>
 *     <li>Distance from Main Sequence (<b>D</b>): {@code | A + I - 1 |}, i.e. the normalized distance from the ideal line between {@code (A=1, I=0)}
 *         and {@code (A=0, I=1)}</li>
 * </ul>
 * <br>
 * As an example take
 * <pre><code>
 * A -&gt; B -&gt; C
 * D -&gt; B
 * </code></pre>
 * Then {@code Ce(B) = 1, Ca(B) = 2, I(B) = 1 / (1 + 2) = 0.33}. Assume {@code 1/3} of the classes in the component would
 * be abstract, then {@code A(B) = 0.33}, thus {@code D(B) = | 0.33 + 0.33 - 1 | = 0.33}.
 * <br><br>
 * Martin's thesis about these metrics is, that the more stable (low {@code I} value) a component gets, the higher the
 * Abstractness ({@code A} value) should be (thus the Distance from the Main Sequence).
 * <br><br>
 * Note: As an adjustment to the original definitions of these metrics we only consider public classes
 * to calculate the abstractness of a component. The background is, that these metrics analyse the maintainability
 * of components in relation to their dependencies. Internals of a component (non-public classes) do not
 * influence the coupling of two components, since there cannot be any dependencies to these components
 * from the outside. Thus they can be freely modified, no matter how many incoming dependencies there are,
 * or if they are abstract or not.
 */
@PublicAPI(usage = ACCESS)
public final class ComponentDependencyMetrics {
    private final Map<String, SingleComponentMetrics> metricsByComponentIdentifier;

    ComponentDependencyMetrics(MetricsComponents<JavaClass> components, Function<JavaClass, Collection<JavaClass>> getDependencies) {
        MetricsComponentDependencyGraph<JavaClass> graph = MetricsComponentDependencyGraph.of(components, getDependencies);
        ImmutableMap.Builder<String, SingleComponentMetrics> metricsByComponentIdentifierBuilder = ImmutableMap.builder();
        for (MetricsComponent<JavaClass> component : components) {
            metricsByComponentIdentifierBuilder.put(component.getIdentifier(), new SingleComponentMetrics(component, graph));
        }
        this.metricsByComponentIdentifier = metricsByComponentIdentifierBuilder.build();
    }

    /**
     * The {@link ComponentDependencyMetrics Efferent Coupling (Ce)} of the components.
     *
     * @see ComponentDependencyMetrics
     */
    @PublicAPI(usage = ACCESS)
    public int getEfferentCoupling(String componentIdentifier) {
        checkComponentExists(componentIdentifier);
        return metricsByComponentIdentifier.get(componentIdentifier).getEfferentCoupling();
    }

    /**
     * The {@link ComponentDependencyMetrics Afferent Coupling (Ca)} of the components.
     *
     * @see ComponentDependencyMetrics
     */
    @PublicAPI(usage = ACCESS)
    public int getAfferentCoupling(String componentIdentifier) {
        checkComponentExists(componentIdentifier);
        return metricsByComponentIdentifier.get(componentIdentifier).getAfferentCoupling();
    }

    /**
     * The {@link ComponentDependencyMetrics Instability (I)} of the components.
     *
     * @see ComponentDependencyMetrics
     */
    @PublicAPI(usage = ACCESS)
    public double getInstability(String componentIdentifier) {
        checkComponentExists(componentIdentifier);
        return metricsByComponentIdentifier.get(componentIdentifier).getInstability();
    }

    /**
     * The {@link ComponentDependencyMetrics Abstractness (A)} of the components.
     *
     * @see ComponentDependencyMetrics
     */
    @PublicAPI(usage = ACCESS)
    public double getAbstractness(String componentIdentifier) {
        checkComponentExists(componentIdentifier);
        return metricsByComponentIdentifier.get(componentIdentifier).getAbstractness();
    }

    /**
     * The {@link ComponentDependencyMetrics Normalized Distance from Main Sequence (D)} of the components.
     *
     * @see ComponentDependencyMetrics
     */
    @PublicAPI(usage = ACCESS)
    public double getNormalizedDistanceFromMainSequence(String componentIdentifier) {
        checkComponentExists(componentIdentifier);
        return metricsByComponentIdentifier.get(componentIdentifier).getNormalizedDistanceFromMainSequence();
    }

    private void checkComponentExists(String componentIdentifier) {
        checkArgument(metricsByComponentIdentifier.containsKey(componentIdentifier),
                "Unknown component with identifier '" + componentIdentifier + "'");
    }

    private static class SingleComponentMetrics {
        private final int efferentCoupling;
        private final int afferentCoupling;
        private final double instability;
        private final double abstractness;
        private final double normalizedDistanceFromMainSequence;

        SingleComponentMetrics(MetricsComponent<JavaClass> component, MetricsComponentDependencyGraph<JavaClass> graph) {
            efferentCoupling = graph.getDirectDependenciesFrom(component).size();
            afferentCoupling = graph.getDirectDependenciesTo(component).size();
            instability = divideSafely(efferentCoupling, efferentCoupling + afferentCoupling, 1);
            ContainedPublicClasses classes = new ContainedPublicClasses(component);
            abstractness = divideSafely(classes.numberOfAbstractClasses, classes.numberOfAllClasses, 0);
            normalizedDistanceFromMainSequence = Math.abs(instability + abstractness - 1);
        }

        int getEfferentCoupling() {
            return efferentCoupling;
        }

        int getAfferentCoupling() {
            return afferentCoupling;
        }

        double getInstability() {
            return instability;
        }

        double getAbstractness() {
            return abstractness;
        }

        double getNormalizedDistanceFromMainSequence() {
            return normalizedDistanceFromMainSequence;
        }

        private static double divideSafely(int dividend, int divisor, double replacementValue) {
            return divisor != 0 ? ((double) dividend) / divisor : replacementValue;
        }

        private static class ContainedPublicClasses {
            final int numberOfAbstractClasses;
            final int numberOfAllClasses;

            ContainedPublicClasses(MetricsComponent<JavaClass> component) {
                int numberOfAbstractClasses = 0;
                int numberOfAllClasses = 0;
                for (JavaClass javaClass : component) {
                    if (javaClass.getModifiers().contains(PUBLIC)) {
                        numberOfAllClasses++;
                        if (javaClass.getModifiers().contains(ABSTRACT)) {
                            numberOfAbstractClasses++;
                        }
                    }
                }
                this.numberOfAbstractClasses = numberOfAbstractClasses;
                this.numberOfAllClasses = numberOfAllClasses;
            }
        }
    }
}
