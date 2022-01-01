/*
 * Copyright 2014-2022 TNG Technology Consulting GmbH
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

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.Function;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static java.util.Collections.singleton;

/**
 * Calculates architecture metrics as defined by John Lakos in his book "Large-Scale C++ Software Design".<br>
 * To calculate these metrics every component is assigned a "dependsOn" value that represents the number
 * of other components that this component can reach transitively, including itself. Take e.g. components
 * <pre><code>
 * A -&gt; B -&gt; C
 * A -&gt; D
 * </code></pre>
 * Then {@code dependsOn(A) = 4}, {@code dependsOn(B) = 2}, {@code dependsOn(C) = 1}, {@code dependsOn(D) = 1}<br>
 * The Lakos metrics are then calculated as:
 * <p>
 *     <ul>
 *         <li> Cumulative Component Dependency (<b>CCD</b>):
 *              The sum of all dependsOn values of all components </li>
 *         <li> Average Component Dependency (<b>ACD</b>):
 *              The CCD divided by the number of all components </li>
 *         <li> Relative Average Component Dependency (<b>RACD</b>):
 *              The ACD divided by the number of all components </li>
 *         <li> Normalized Cumulative Component Dependency (<b>NCCD</b>):
 *              The CCD of the system divided by the CCD of a balanced binary tree with the same number of components </li>
 *     </ul>
 * </p>
 * Given the example graph above we would obtain
 * {@code CCD = 4 + 2 + 1 + 1 = 8}, {@code ACD = 8 / 4 = 2}, {@code RACD = 2 / 4 = 0.5}, {@code NCCD = 8 / 8 = 1.0}
 */
@PublicAPI(usage = ACCESS)
public final class LakosMetrics {
    private final int cumulativeComponentDependency;
    private final double averageComponentDependency;
    private final double relativeAverageComponentDependency;
    private final double normalizedCumulativeComponentDependency;

    <T> LakosMetrics(Collection<MetricsComponent<T>> components, Function<T, Collection<T>> getDependencies) {
        int cumulativeComponentDependency = 0;
        MetricsComponentDependencyGraph<T> graph = MetricsComponentDependencyGraph.of(components, getDependencies);
        for (MetricsComponent<T> component : components) {
            cumulativeComponentDependency += 1 + getNumberOfTransitiveDependencies(graph, component);
        }
        this.cumulativeComponentDependency = cumulativeComponentDependency;
        this.averageComponentDependency = ((double) cumulativeComponentDependency) / components.size();
        this.relativeAverageComponentDependency = averageComponentDependency / components.size();
        this.normalizedCumulativeComponentDependency =
                ((double) cumulativeComponentDependency) / calculateCumulativeComponentDependencyOfBinaryTree(components.size());
    }

    private <T> int getNumberOfTransitiveDependencies(MetricsComponentDependencyGraph<T> graph, MetricsComponent<T> component) {
        Sets.SetView<MetricsComponent<T>> transitiveDependenciesWithoutSelf = Sets.difference(graph.getTransitiveDependenciesOf(component), singleton(component));
        return transitiveDependenciesWithoutSelf.size();
    }

    private int calculateCumulativeComponentDependencyOfBinaryTree(int treeSize) {
        int ccdOfBinaryTree = 0;
        int level = 1;
        int maxNodesUpToCurrentLevel = 1;
        for (int currentNode = 1; currentNode <= treeSize; currentNode++) {
            if (currentNode > maxNodesUpToCurrentLevel) {
                level++;
                maxNodesUpToCurrentLevel += Math.pow(2, level - 1);
            }
            ccdOfBinaryTree += level;
        }
        return ccdOfBinaryTree;
    }

    /**
     * The {@link LakosMetrics Cumulative Component Dependency (CCD)} of the components.
     *
     * @see LakosMetrics
     */
    @PublicAPI(usage = ACCESS)
    public int getCumulativeComponentDependency() {
        return cumulativeComponentDependency;
    }

    /**
     * The {@link LakosMetrics Average Component Dependency (ACD)} of the components.
     *
     * @see LakosMetrics
     */
    @PublicAPI(usage = ACCESS)
    public double getAverageComponentDependency() {
        return averageComponentDependency;
    }

    /**
     * The {@link LakosMetrics Relative Average Component Dependency (RACD)} of the components.
     *
     * @see LakosMetrics
     */
    @PublicAPI(usage = ACCESS)
    public double getRelativeAverageComponentDependency() {
        return relativeAverageComponentDependency;
    }

    /**
     * The {@link LakosMetrics Normalized Cumulative Component Dependency (NCCD)} of the components.
     *
     * @see LakosMetrics
     */
    @PublicAPI(usage = ACCESS)
    public double getNormalizedCumulativeComponentDependency() {
        return normalizedCumulativeComponentDependency;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("cumulativeComponentDependency", cumulativeComponentDependency)
                .add("averageComponentDependency", averageComponentDependency)
                .add("relativeAverageComponentDependency", relativeAverageComponentDependency)
                .add("normalizedCumulativeComponentDependency", normalizedCumulativeComponentDependency)
                .toString();
    }
}
