package com.tngtech.archunit.library;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.Priority;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.Lists.newArrayList;
import static com.tngtech.archunit.lang.ArchRule.Assertions.assertNoViolation;
import static com.tngtech.archunit.lang.ArchRule.Definition.all;
import static com.tngtech.archunit.lang.ArchRule.Definition.classes;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyBeAccessedByAnyPackage;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.resideInAnyPackage;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;

public class Architectures {
    /**
     * Can be used to assert a typical layered architecture, e.g. with an UI layer, a business logic layer and
     * a persistence layer, where specific access rules should be adhered to, like UI may not access persistence
     * and each layer may only access lower layers, i.e. UI -> business logic -> persistence.
     * <br/><br/>
     * A layered architecture can for example be defined like this:
     * <pre><code>layeredArchitecture()
     * .layer("UI").definedBy("my.application.ui..")
     * .layer("Business Logic").definedBy("my.application.domain..")
     * .layer("Persistence").definedBy("my.application.persistence..")
     *
     * .whereLayer("UI").mayNotBeAccessedByAnyLayer()
     * .whereLayer("Business Logic").mayOnlyBeAccessedByLayers("UI")
     * .whereLayer("Persistence").mayOnlyBeAccessedByLayers("Business Logic")
     * </code></pre>
     * NOTE: Principally it would be possible to assert such an architecture in a white list or black list way.
     * <br/>I.e. one can specify layer 'Persistence' MAY ONLY be accessed by layer 'Business Logic' (white list), or
     * layer 'Persistence' MAY NOT access layer 'Business Logic' AND MAY NOT access layer 'UI' (black list).<br/>
     * {@link LayeredArchitecture LayeredArchitecture} only supports the white list way, because it prevents detours "outside of
     * the architecture", e.g.<br/>
     * 'Persistence' -> 'my.application.somehelper' -> 'Business Logic'<br/>
     * The white list way enforces, that every class that wants to interact with classes inside of
     * the layered architecture must be part of the layered architecture itself and thus adhere to the same rules.
     *
     * @return An {@link ArchRule} enforcing the specified layered architecture
     */
    public static LayeredArchitecture layeredArchitecture() {
        return new LayeredArchitecture();
    }

    public static class LayeredArchitecture implements ArchRule {
        private Map<String, LayerDefinition> layerDefinitions = new LinkedHashMap<>();
        private Set<LayerDependencySpecification> dependencySpecifications = new LinkedHashSet<>();

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
        public String getDescription() {
            List<String> lines = newArrayList("Layered architecture consisting of");
            for (LayerDefinition definition : layerDefinitions.values()) {
                lines.add(definition.toString());
            }
            for (LayerDependencySpecification specification : dependencySpecifications) {
                lines.add(specification.toString());
            }
            return Joiner.on(lineSeparator()).join(lines);
        }

        @Override
        public EvaluationResult evaluate(JavaClasses classes) {
            EvaluationResult result = new EvaluationResult(this, Priority.MEDIUM);
            for (LayerDependencySpecification specification : dependencySpecifications) {
                SortedSet<String> packagesOfOwnLayer = packagesOf(specification.layerName);
                SortedSet<String> packagesOfAllowedAccessors = packagesOf(specification.allowedAccessors);
                packagesOfAllowedAccessors.addAll(packagesOfOwnLayer);

                EvaluationResult partial = all(classes().that(resideInAnyPackage(toArray(packagesOfOwnLayer))))
                        .should(onlyBeAccessedByAnyPackage(toArray(packagesOfAllowedAccessors)))
                        .evaluate(classes);

                result.add(partial);
            }
            return result;
        }

        @Override
        public void check(JavaClasses classes) {
            assertNoViolation(evaluate(classes));
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

            @Override
            public String toString() {
                return String.format("layer '%s' ('%s')", name, Joiner.on("', '").join(packageIdentifiers));
            }
        }

        public class LayerDependencySpecification {
            private final String layerName;
            private final Set<String> allowedAccessors = new LinkedHashSet<>();
            private String descriptionSuffix;

            private LayerDependencySpecification(String layerName) {
                this.layerName = layerName;
            }

            public LayeredArchitecture mayNotBeAccessedByAnyLayer() {
                descriptionSuffix = "may not be accessed by any layer";
                return LayeredArchitecture.this.addDependencySpecification(this);
            }

            public LayeredArchitecture mayOnlyBeAccessedByLayers(String... layerNames) {
                allowedAccessors.addAll(asList(layerNames));
                descriptionSuffix = String.format("may only be accessed by layers ['%s']",
                        Joiner.on("', '").join(allowedAccessors));
                return LayeredArchitecture.this.addDependencySpecification(this);
            }

            @Override
            public String toString() {
                return String.format("where layer '%s' %s", layerName, descriptionSuffix);
            }
        }
    }
}
