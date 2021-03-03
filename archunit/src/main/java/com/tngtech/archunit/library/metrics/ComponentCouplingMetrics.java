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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.library.metrics.components.MetricsComponent;
import com.tngtech.archunit.library.metrics.components.MetricsComponentDependency;
import com.tngtech.archunit.library.metrics.components.MetricsComponents;
import com.tngtech.archunit.library.metrics.rendering.Diagram;

import static com.tngtech.archunit.core.domain.JavaModifier.ABSTRACT;
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;
import static com.tngtech.archunit.library.metrics.MathUtils.divideSafely;
import static java.lang.System.lineSeparator;

public class ComponentCouplingMetrics {
    private final Map<String, ComponentCoupling> couplings;

    public ComponentCouplingMetrics(MetricsComponents<JavaClass> components) {
        ImmutableMap.Builder<String, ComponentCoupling> couplingsBuilder = ImmutableMap.builder();
        for (MetricsComponent<JavaClass> component : components) {
            couplingsBuilder.put(component.getIdentifier(), new ComponentCoupling(component));
        }
        couplings = couplingsBuilder.build();
    }

    public Diagram toDiagram() {
        Diagram.Builder diagramBuilder = Diagram.builder();
        for (Map.Entry<String, ComponentCoupling> couplingEntry : couplings.entrySet()) {
            diagramBuilder.addComponent(couplingEntry.getKey(), couplingEntry.getValue().describe());
            for (MetricsComponentDependency<?> dependency : couplingEntry.getValue().getDependenciesFromSelf()) {
                diagramBuilder.addDependency(dependency.getOrigin().getIdentifier(), dependency.getTarget().getIdentifier());
            }
        }
        return diagramBuilder.build();
    }

    private static int countElementDependencies(Set<? extends MetricsComponentDependency<?>> componentDependencies) {
        Set<Object> result = new HashSet<>();
        for (MetricsComponentDependency<?> componentDependency : componentDependencies) {
            result.addAll(componentDependency);
        }
        return result.size();
    }

    public static ComponentCouplingMetrics of(MetricsComponents<JavaClass> components) {
        return new ComponentCouplingMetrics(components);
    }

    private static class ComponentCoupling {
        private static final DecimalFormat TWO_DIGITS = new DecimalFormat("0.00");

        private final MetricsComponent<JavaClass> component;
        private final AfferentCoupling afferentCoupling;
        private final EfferentCoupling efferentCoupling;
        private final Instability instability;
        private final Abstractness abstractness;
        private final DistanceFromMainSequence distanceFromMainSequence;

        private ComponentCoupling(MetricsComponent<JavaClass> component) {
            this.component = component;
            afferentCoupling = new AfferentCoupling(component);
            efferentCoupling = new EfferentCoupling(component);
            instability = new Instability(afferentCoupling, efferentCoupling);
            abstractness = new Abstractness(component);
            distanceFromMainSequence = new DistanceFromMainSequence(instability, abstractness);
        }

        public String getName() {
            return component.getName();
        }

        public Set<MetricsComponentDependency<JavaClass>> getDependenciesFromSelf() {
            return component.getComponentDependenciesFromSelf();
        }

        public String describe() {
            List<String> lines = new ArrayList<>();
            lines.add("Name: " + component.getName());
            lines.add("Ca: " + afferentCoupling.componentCa + " (" + afferentCoupling.elementCa + ")");
            lines.add("Ce: " + efferentCoupling.componentCe + " (" + efferentCoupling.elementCe + ")");
            lines.add("Instability: " + TWO_DIGITS.format(instability.componentInstability) + " (" + TWO_DIGITS.format(instability.elementInstability) + ")");
            lines.add("Abstractness: " + TWO_DIGITS.format(abstractness.value));
            lines.add("Distance from the Main Sequence: "
                    + TWO_DIGITS.format(distanceFromMainSequence.componentDistance) + " (" + TWO_DIGITS.format(distanceFromMainSequence.elementDistance) + ")");
            return Joiner.on(lineSeparator()).join(lines);
        }
    }

    private static class AfferentCoupling {
        final int componentCa;
        final int elementCa;

        AfferentCoupling(MetricsComponent<?> component) {
            componentCa = component.getComponentDependenciesToSelf().size();
            elementCa = countElementDependencies(component.getComponentDependenciesToSelf());
        }
    }

    private static class EfferentCoupling {
        final int componentCe;
        final int elementCe;

        EfferentCoupling(MetricsComponent<?> component) {
            componentCe = component.getComponentDependenciesFromSelf().size();
            elementCe = countElementDependencies(component.getComponentDependenciesFromSelf());
        }
    }

    private static class Instability {
        final double componentInstability;
        final double elementInstability;

        Instability(AfferentCoupling afferentCoupling, EfferentCoupling efferentCoupling) {
            componentInstability = divideSafely(efferentCoupling.componentCe, efferentCoupling.componentCe + afferentCoupling.componentCa);
            elementInstability = divideSafely(efferentCoupling.elementCe, efferentCoupling.elementCe + afferentCoupling.elementCa);
        }
    }

    private static class Abstractness {
        final double value;

        Abstractness(MetricsComponent<JavaClass> component) {
            Set<JavaClass> publicClasses = filterClassesWithModifier(component, PUBLIC);
            Set<JavaClass> abstractClasses = filterClassesWithModifier(publicClasses, ABSTRACT);
            value = divideSafely(abstractClasses.size(), publicClasses.size());
        }

        private Set<JavaClass> filterClassesWithModifier(Iterable<JavaClass> classes, JavaModifier modifier) {
            Set<JavaClass> publicClasses = new HashSet<>();
            for (JavaClass javaClass : classes) {
                if (javaClass.getModifiers().contains(modifier)) {
                    publicClasses.add(javaClass);
                }
            }
            return publicClasses;
        }
    }

    private static class DistanceFromMainSequence {
        final double componentDistance;
        final double elementDistance;

        DistanceFromMainSequence(Instability instability, Abstractness abstractness) {
            componentDistance = Math.abs(instability.componentInstability + abstractness.value - 1);
            elementDistance = Math.abs(instability.elementInstability + abstractness.value - 1);
        }
    }
}
