package com.tngtech.archunit.library;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;

import static com.google.common.base.Preconditions.checkArgument;
import static com.tngtech.archunit.lang.ArchRule.Definition.all;
import static com.tngtech.archunit.lang.ArchRule.Definition.classes;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyBeAccessedByAnyPackage;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.resideInAnyPackage;

public class Architectures {
    public static LayeredArchitecture layeredArchitecture() {
        return new LayeredArchitecture();
    }

    public static class LayeredArchitecture implements ArchRule {
        private Map<String, LayerDefinition> layerDefinitions = new HashMap<>();
        private Set<LayerDependencySpecification> dependencySpecifications = new HashSet<>();

        private LayeredArchitecture() {
        }

        private LayeredArchitecture addLayerDefinition(LayerDefinition definition) {
            layerDefinitions.put(definition.name, definition);
            return this;
        }

        private LayeredArchitecture addDependencySpecification(LayerDependencySpecification dependencySpecification) {
            dependencySpecifications.add(dependencySpecification);
            return this;
        }

        public LayerDefinition layer(String name) {
            return new LayerDefinition(name);
        }

        @Override
        public void check(JavaClasses classes) {
            for (LayerDependencySpecification specification : dependencySpecifications) {
                SortedSet<String> packagesOfOwnLayer = packagesOf(specification.layerName);
                SortedSet<String> packagesOfAllowedAccessors = packagesOf(specification.allowedAccessors);
                packagesOfAllowedAccessors.addAll(packagesOfOwnLayer);

                all(classes().that(resideInAnyPackage(toArray(packagesOfOwnLayer))))
                        .should(onlyBeAccessedByAnyPackage(toArray(packagesOfAllowedAccessors))).check(classes);
            }
        }

        private String[] toArray(Set<String> strings) {
            return strings.toArray(new String[strings.size()]);
        }

        private SortedSet<String> packagesOf(String layerName) {
            return packagesOf(Collections.singleton(layerName));
        }

        private SortedSet<String> packagesOf(Set<String> allowedAccessorLayerNames) {
            SortedSet<String> packageIdentifiers = new TreeSet<>();
            for (String accessor : allowedAccessorLayerNames) {
                packageIdentifiers.addAll(layerDefinitions.get(accessor).packageIdentifiers);
            }
            return packageIdentifiers;
        }

        public LayerDependencySpecification whereLayer(String name) {
            checkArgument(layerDefinitions.containsKey(name), "There is no layer named '%s'", name);
            return new LayerDependencySpecification(name);
        }

        public class LayerDefinition {
            private final String name;
            private Set<String> packageIdentifiers;

            private LayerDefinition(String name) {
                this.name = name;
            }

            public LayeredArchitecture definedBy(String... packageIdentifiers) {
                this.packageIdentifiers = ImmutableSet.copyOf(packageIdentifiers);
                return LayeredArchitecture.this.addLayerDefinition(this);
            }
        }

        public class LayerDependencySpecification {
            private final String layerName;
            private final Set<String> allowedAccessors = new HashSet<>();

            private LayerDependencySpecification(String layerName) {
                this.layerName = layerName;
            }

            public LayeredArchitecture mayNotBeAccessedByAnyLayer() {
                return LayeredArchitecture.this.addDependencySpecification(this);
            }

            public LayeredArchitecture mayOnlyBeAccessedByLayers(String... layerNames) {
                allowedAccessors.addAll(ImmutableSet.copyOf(layerNames));
                return LayeredArchitecture.this.addDependencySpecification(this);
            }
        }
    }
}
