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

import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.tngtech.archunit.library.metrics.components.MetricsComponent;
import com.tngtech.archunit.library.metrics.components.MetricsComponentDependency;
import com.tngtech.archunit.library.metrics.components.MetricsComponents;
import com.tngtech.archunit.library.metrics.rendering.AsciiDocTable;
import com.tngtech.archunit.library.metrics.rendering.Diagram;

import static com.tngtech.archunit.library.metrics.MathUtils.divideSafely;
import static java.lang.System.lineSeparator;

public class LakosMetrics {
    private static final DecimalFormat TWO_DIGITS = new DecimalFormat("0.00");

    private final MetricsComponents<?> components;
    private final Map<String, DependsUpon> dependsUponByIdentifier;
    private final Map<String, UsedFrom> usedFromByIdentifier;
    private final CumulativeComponentDependency cumulativeComponentDependency;
    private final AverageComponentDependency averageComponentDependency;
    private final RelativeAverageComponentDependency relativeAverageComponentDependency;

    private LakosMetrics(MetricsComponents<?> components) {
        this.components = components;
        dependsUponByIdentifier = createDependsUpon(components);
        usedFromByIdentifier = createUsedFrom(components);
        cumulativeComponentDependency = new CumulativeComponentDependency(dependsUponByIdentifier.values());
        averageComponentDependency = new AverageComponentDependency(cumulativeComponentDependency, components);
        relativeAverageComponentDependency = new RelativeAverageComponentDependency(averageComponentDependency, components);
    }

    public Diagram toDiagram() {
        Diagram.Builder diagramBuilder = Diagram.builder();
        for (MetricsComponent<?> component : components) {
            diagramBuilder.addComponent(component.getIdentifier(), String.format("%s%n%s%n%s",
                    component.getName(),
                    dependsUponByIdentifier.get(component.getIdentifier()).render(),
                    usedFromByIdentifier.get(component.getIdentifier()).render()));
            for (MetricsComponentDependency<?> dependency : component.getComponentDependenciesFromSelf()) {
                diagramBuilder.addDependency(dependency.getOrigin().getIdentifier(), dependency.getTarget().getIdentifier());
            }
        }
        diagramBuilder.addLegend(Joiner.on(lineSeparator()).join(
                "CCD: " + cumulativeComponentDependency.value,
                "ACD: " + averageComponentDependency.formattedValue(),
                "RACD: " + relativeAverageComponentDependency.formattedValue()));
        return diagramBuilder.build();
    }

    public AsciiDocTable toAsciiDocTable() {
        AsciiDocTable.Creator tableCreator = AsciiDocTable.intro()
                .addLine("CCD: " + cumulativeComponentDependency.value)
                .addLine("ACD: " + averageComponentDependency.formattedValue())
                .addLine("RACD: " + relativeAverageComponentDependency.formattedValue())
                .header().addColumnValue("Name").addColumnValue("Depends Upon").addColumnValue("Used From").end();

        for (MetricsComponent<?> component : sortByDependsUpon(components)) {
            tableCreator.row()
                    .addColumnValue(component.getName())
                    .addColumnValue(dependsUponByIdentifier.get(component.getIdentifier()).render())
                    .addColumnValue(usedFromByIdentifier.get(component.getIdentifier()).render())
                    .end();
        }
        return tableCreator.create();
    }

    private List<? extends MetricsComponent<?>> sortByDependsUpon(MetricsComponents<?> components) {
        return FluentIterable.from(components).toSortedList(new Comparator<MetricsComponent<?>>() {
            @Override
            public int compare(MetricsComponent<?> first, MetricsComponent<?> second) {
                return ComparisonChain.start()
                        .compare(
                                dependsUponByIdentifier.get(second.getIdentifier()).value(),
                                dependsUponByIdentifier.get(first.getIdentifier()).value())
                        .compare(first.getName(), second.getName())
                        .result();
            }
        });
    }

    private Map<String, DependsUpon> createDependsUpon(MetricsComponents<?> components) {
        ImmutableMap.Builder<String, DependsUpon> result = ImmutableMap.builder();
        for (MetricsComponent<?> component : components) {
            result.put(component.getIdentifier(), new DependsUpon(component));
        }
        return result.build();
    }

    private Map<String, UsedFrom> createUsedFrom(MetricsComponents<?> components) {
        ImmutableMap.Builder<String, UsedFrom> result = ImmutableMap.builder();
        for (MetricsComponent<?> component : components) {
            result.put(component.getIdentifier(), new UsedFrom(component));
        }
        return result.build();
    }

    public static LakosMetrics of(MetricsComponents<?> components) {
        return new LakosMetrics(components);
    }

    private static class DependsUpon {
        private final Set<MetricsComponent<?>> dependsUponComponents;

        DependsUpon(MetricsComponent<?> component) {
            Set<MetricsComponent<?>> dependsUponComponents = Sets.<MetricsComponent<?>>newHashSet(component);
            addAllTransitiveComponentDependenciesFromSelf(dependsUponComponents, component);
            this.dependsUponComponents = ImmutableSet.copyOf(dependsUponComponents);
        }

        private static void addAllTransitiveComponentDependenciesFromSelf(Set<MetricsComponent<?>> reachableComponents, MetricsComponent<?> component) {
            for (MetricsComponentDependency<?> dependency : component.getComponentDependenciesFromSelf()) {
                if (!reachableComponents.contains(dependency.getTarget())) {
                    reachableComponents.add(dependency.getTarget());
                    addAllTransitiveComponentDependenciesFromSelf(reachableComponents, dependency.getTarget());
                }
            }
        }

        int value() {
            return dependsUponComponents.size();
        }

        String render() {
            return "Depends Upon: " + value();
        }
    }

    private static class UsedFrom {
        private final Set<MetricsComponent<?>> usedFromComponents;

        UsedFrom(MetricsComponent<?> component) {
            Set<MetricsComponent<?>> dependsUponComponents = Sets.<MetricsComponent<?>>newHashSet(component);
            addAllTransitiveComponentDependenciesToSelf(dependsUponComponents, component);
            this.usedFromComponents = ImmutableSet.copyOf(dependsUponComponents);
        }

        private static void addAllTransitiveComponentDependenciesToSelf(Set<MetricsComponent<?>> reachableComponents, MetricsComponent<?> component) {
            for (MetricsComponentDependency<?> dependency : component.getComponentDependenciesToSelf()) {
                if (!reachableComponents.contains(dependency.getOrigin())) {
                    reachableComponents.add(dependency.getOrigin());
                    addAllTransitiveComponentDependenciesToSelf(reachableComponents, dependency.getOrigin());
                }
            }
        }

        String render() {
            return "Used From: " + usedFromComponents.size();
        }
    }

    private static class CumulativeComponentDependency {
        private final int value;

        private CumulativeComponentDependency(Iterable<DependsUpon> allDependsUpons) {
            int acc = 0;
            for (DependsUpon dependsUpon : allDependsUpons) {
                acc += dependsUpon.value();
            }
            value = acc;
        }
    }

    private static class AverageComponentDependency {
        private final double value;

        private AverageComponentDependency(CumulativeComponentDependency ccd, MetricsComponents<?> allComponents) {
            this.value = divideSafely(ccd.value, allComponents.size());
        }

        String formattedValue() {
            return TWO_DIGITS.format(value);
        }
    }

    private static class RelativeAverageComponentDependency {
        private final double value;

        private RelativeAverageComponentDependency(AverageComponentDependency acd, MetricsComponents<?> allComponents) {
            this.value = divideSafely(acd.value, allComponents.size());
        }

        String formattedValue() {
            return TWO_DIGITS.format(value);
        }
    }
}
