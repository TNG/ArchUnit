/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.library.plantuml;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.PackageMatcher;
import com.tngtech.archunit.core.domain.JavaClass;

import static com.google.common.collect.Iterables.getOnlyElement;

class JavaClassDiagramAssociation {
    private final Set<AssociatedComponent> components;

    JavaClassDiagramAssociation(PlantUmlDiagram diagram) {
        ImmutableSet.Builder<AssociatedComponent> components = ImmutableSet.builder();
        validateStereotypes(diagram);
        for (PlantUmlComponent component : diagram.getAllComponents()) {
            components.add(new AssociatedComponent(component));
        }
        this.components = components.build();
    }

    private void validateStereotypes(PlantUmlDiagram plantUmlDiagram) {
        Set<Stereotype> visited = new HashSet<>();
        for (PlantUmlComponent component : plantUmlDiagram.getAllComponents()) {
            for (Stereotype stereotype : component.getStereotypes()) {
                if (visited.contains(stereotype)) {
                    throw new IllegalDiagramException(String.format("Stereotype '%s' should be unique", stereotype.asString()));
                }
                visited.add(stereotype);
            }
        }
    }

    Set<String> getTargetPackageIdentifiers(final JavaClass javaClass) {
        ImmutableSet.Builder<String> result = ImmutableSet.builder();
        for (PlantUmlComponent target : getComponentOf(javaClass).getDependencies()) {
            result.addAll(getPackageIdentifiersFromComponentOf(target));
        }
        return result.build();
    }

    Set<String> getPackageIdentifiersFromComponentOf(JavaClass javaClass) {
        return getPackageIdentifiersFromComponentOf(getComponentOf(javaClass));
    }

    private Set<String> getPackageIdentifiersFromComponentOf(PlantUmlComponent component) {
        ImmutableSet.Builder<String> result = ImmutableSet.builder();
        for (Stereotype stereotype : component.getStereotypes()) {
            result.add(stereotype.asString());
        }
        return result.build();
    }

    private PlantUmlComponent getComponentOf(final JavaClass javaClass) {
        Set<PlantUmlComponent> associatedComponents = getAssociatedComponents(javaClass);

        if (associatedComponents.size() > 1) {
            throw new ComponentIntersectionException(
                    String.format("Class %s may not be contained in more than one component, but is contained in [%s]",
                            javaClass.getName(),
                            Joiner.on(", ").join(getComponentNames(associatedComponents))));
        } else if (associatedComponents.isEmpty()) {
            throw new IllegalStateException(String.format("Class %s is not contained in any component", javaClass.getName()));
        }

        return getOnlyElement(associatedComponents);
    }

    boolean contains(JavaClass javaClass) {
        return !getAssociatedComponents(javaClass).isEmpty();
    }

    private Set<PlantUmlComponent> getAssociatedComponents(JavaClass javaClass) {
        ImmutableSet.Builder<PlantUmlComponent> result = ImmutableSet.builder();
        for (AssociatedComponent component : components) {
            if (component.contains(javaClass)) {
                result.add(component.asPlantUmlComponent());
            }
        }
        return result.build();
    }

    private Set<String> getComponentNames(Set<PlantUmlComponent> associatedComponents) {
        Set<String> associatedComponentNames = new TreeSet<>();
        for (PlantUmlComponent associatedComponent : associatedComponents) {
            associatedComponentNames.add(associatedComponent.getComponentName().asString());
        }
        return associatedComponentNames;
    }

    private static class AssociatedComponent {
        private final PlantUmlComponent component;
        private final Set<PackageMatcher> packageMatchers;

        private AssociatedComponent(PlantUmlComponent component) {
            this.component = component;
            ImmutableSet.Builder<PackageMatcher> packageMatchers = ImmutableSet.builder();
            for (Stereotype stereotype : component.getStereotypes()) {
                packageMatchers.add(PackageMatcher.of(stereotype.asString()));
            }
            this.packageMatchers = packageMatchers.build();
        }

        private boolean contains(JavaClass javaClass) {
            for (PackageMatcher packageMatcher : packageMatchers) {
                if (packageMatcher.matches(javaClass.getPackageName())) {
                    return true;
                }
            }
            return false;
        }

        PlantUmlComponent asPlantUmlComponent() {
            return component;
        }
    }
}