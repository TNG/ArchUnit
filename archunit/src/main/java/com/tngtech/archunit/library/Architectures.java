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
package com.tngtech.archunit.library;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Joiner;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.PredicateAggregator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.base.DescribedPredicate.alwaysFalse;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependency;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependencyOrigin;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyHaveDependentsWhere;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

/**
 * Offers convenience to assert typical architectures, like a {@link #layeredArchitecture()}.
 */
public final class Architectures {
    private Architectures() {
    }

    /**
     * Can be used to assert a typical layered architecture, e.g. with an UI layer, a business logic layer and
     * a persistence layer, where specific access rules should be adhered to, like UI may not access persistence
     * and each layer may only access lower layers, i.e. UI &rarr; business logic &rarr; persistence.
     * <br><br>
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
     * <br>I.e. one can specify layer 'Persistence' MAY ONLY be accessed by layer 'Business Logic' (white list), or
     * layer 'Persistence' MAY NOT access layer 'Business Logic' AND MAY NOT access layer 'UI' (black list).<br>
     * {@link LayeredArchitecture LayeredArchitecture} only supports the white list way, because it prevents detours "outside of
     * the architecture", e.g.<br>
     * 'Persistence' &rarr; 'my.application.somehelper' &rarr; 'Business Logic'<br>
     * The white list way enforces that every class that wants to interact with classes inside of
     * the layered architecture must be part of the layered architecture itself and thus adhere to the same rules.
     *
     * @return An {@link ArchRule} enforcing the specified layered architecture
     */
    @PublicAPI(usage = ACCESS)
    public static LayeredArchitecture layeredArchitecture() {
        return new LayeredArchitecture();
    }

    public static final class LayeredArchitecture implements ArchRule {
        private final LayerDefinitions layerDefinitions;
        private final Set<LayerDependencySpecification> dependencySpecifications;
        private final PredicateAggregator<Dependency> irrelevantDependenciesPredicate;
        private final Optional<String> overriddenDescription;
        private boolean optionalLayers;

        private LayeredArchitecture() {
            this(new LayerDefinitions(),
                    new LinkedHashSet<LayerDependencySpecification>(),
                    new PredicateAggregator<Dependency>().thatORs(),
                    Optional.<String>absent(),
                    false);
        }

        private LayeredArchitecture(LayerDefinitions layerDefinitions,
                Set<LayerDependencySpecification> dependencySpecifications,
                PredicateAggregator<Dependency> irrelevantDependenciesPredicate,
                Optional<String> overriddenDescription,
                boolean optionalLayers) {
            this.layerDefinitions = layerDefinitions;
            this.dependencySpecifications = dependencySpecifications;
            this.irrelevantDependenciesPredicate = irrelevantDependenciesPredicate;
            this.overriddenDescription = overriddenDescription;
            this.optionalLayers = optionalLayers;
        }

        /**
         * By default, layers defined with {@link #layer(String)} must not be empty, i.e. contain at least one class.
         * <br>
         * <code>withOptionalLayers(true)</code> can be used to make all layers optional.<br>
         * <code>withOptionalLayers(false)</code> still allows to define individual optional layers with {@link #optionalLayer(String)}.
         * @see #optionalLayer(String)
         */
        @PublicAPI(usage = ACCESS)
        public LayeredArchitecture withOptionalLayers(boolean optionalLayers) {
            this.optionalLayers = optionalLayers;
            return this;
        }

        private LayeredArchitecture addLayerDefinition(LayerDefinition definition) {
            layerDefinitions.add(definition);
            return this;
        }

        private LayeredArchitecture addDependencySpecification(LayerDependencySpecification dependencySpecification) {
            dependencySpecifications.add(dependencySpecification);
            return this;
        }

        /**
         * Starts the definition of a new layer within the current {@link #layeredArchitecture() LayeredArchitecture}.
         * <br>
         * Unless {@link #withOptionalLayers(boolean) withOptionalLayers(true}} is used, this layer must not be empty.
         * @see #optionalLayer(String)
         */
        @PublicAPI(usage = ACCESS)
        public LayerDefinition layer(String name) {
            return new LayerDefinition(name, false);
        }

        /**
         * Starts the definition of a new optional layer within the current {@link #layeredArchitecture() LayeredArchitecture}.
         * <br>
         * An optional layer will not fail if it is empty, i.e. does not contain any classes.
         * When {@link #withOptionalLayers(boolean) withOptionalLayers(true)} is used, all layers are optional by default,
         * such that there is no difference between {@link #optionalLayer(String)} and {@link #layer(String)} anymore
         */
        @PublicAPI(usage = ACCESS)
        public LayerDefinition optionalLayer(String name) {
            return new LayerDefinition(name, true);
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public String getDescription() {
            if (overriddenDescription.isPresent()) {
                return overriddenDescription.get();
            }

            List<String> lines = newArrayList("Layered architecture consisting of" + (optionalLayers ? " (optional)" : ""));
            for (LayerDefinition definition : layerDefinitions) {
                lines.add(definition.toString());
            }
            for (LayerDependencySpecification specification : dependencySpecifications) {
                lines.add(specification.toString());
            }
            return Joiner.on(lineSeparator()).join(lines);
        }

        @Override
        public String toString() {
            return getDescription();
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public EvaluationResult evaluate(JavaClasses classes) {
            EvaluationResult result = new EvaluationResult(this, Priority.MEDIUM);
            checkEmptyLayers(classes, result);
            for (LayerDependencySpecification specification : dependencySpecifications) {
                result.add(evaluateDependenciesShouldBeSatisfied(classes, specification));
            }
            return result;
        }

        private void checkEmptyLayers(JavaClasses classes, EvaluationResult result) {
            if (!optionalLayers) {
                for (LayerDefinition layerDefinition : layerDefinitions) {
                    if (!layerDefinition.isOptional()) {
                        result.add(evaluateLayersShouldNotBeEmpty(classes, layerDefinition));
                    }
                }
            }
        }

        private EvaluationResult evaluateLayersShouldNotBeEmpty(JavaClasses classes, LayerDefinition layerDefinition) {
            return classes().that(layerDefinitions.containsPredicateFor(layerDefinition.name))
                    .should(notBeEmptyFor(layerDefinition))
                    .evaluate(classes);
        }

        private EvaluationResult evaluateDependenciesShouldBeSatisfied(JavaClasses classes, LayerDependencySpecification specification) {

            return classes().that(layerDefinitions.containsPredicateFor(specification.layerName))
                    .should(onlyHaveDependentsWhere(originMatchesIfDependencyIsRelevant(specification.layerName, specification.allowedAccessors)))
                    .evaluate(classes);
        }

        private DescribedPredicate<Dependency> originMatchesIfDependencyIsRelevant(String ownLayer, Set<String> allowedAccessors) {
            DescribedPredicate<Dependency> originPackageMatches =
                    dependencyOrigin(layerDefinitions.containsPredicateFor(allowedAccessors)).or(dependencyOrigin(layerDefinitions.containsPredicateFor(ownLayer)));

            return irrelevantDependenciesPredicate.isPresent() ?
                    originPackageMatches.or(irrelevantDependenciesPredicate.get()) :
                    originPackageMatches;
        }

        private static ArchCondition<JavaClass> notBeEmptyFor(final LayeredArchitecture.LayerDefinition layerDefinition) {
            return new LayerShouldNotBeEmptyCondition(layerDefinition);
        }

        private static class LayerShouldNotBeEmptyCondition extends ArchCondition<JavaClass> {
            private final LayeredArchitecture.LayerDefinition layerDefinition;
            private boolean empty = true;

            LayerShouldNotBeEmptyCondition(final LayeredArchitecture.LayerDefinition layerDefinition) {
                super("not be empty");
                this.layerDefinition = layerDefinition;
            }

            @Override
            public void check(JavaClass item, ConditionEvents events) {
                empty = false;
            }

            @Override
            public void finish(ConditionEvents events) {
                if (empty) {
                    events.add(violated(layerDefinition, String.format("Layer '%s' is empty", layerDefinition.name)));
                }
            }
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public void check(JavaClasses classes) {
            Assertions.check(this, classes);
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public ArchRule because(String reason) {
            return ArchRule.Factory.withBecause(this, reason);
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public LayeredArchitecture as(String newDescription) {
            return new LayeredArchitecture(
                    layerDefinitions, dependencySpecifications,
                    irrelevantDependenciesPredicate, Optional.of(newDescription), optionalLayers);
        }

        @PublicAPI(usage = ACCESS)
        public LayeredArchitecture ignoreDependency(Class<?> origin, Class<?> target) {
            return ignoreDependency(equivalentTo(origin), equivalentTo(target));
        }

        @PublicAPI(usage = ACCESS)
        public LayeredArchitecture ignoreDependency(String origin, String target) {
            return ignoreDependency(name(origin), name(target));
        }

        @PublicAPI(usage = ACCESS)
        public LayeredArchitecture ignoreDependency(
                DescribedPredicate<? super JavaClass> origin, DescribedPredicate<? super JavaClass> target) {
            return new LayeredArchitecture(
                    layerDefinitions, dependencySpecifications,
                    irrelevantDependenciesPredicate.add(dependency(origin, target)), overriddenDescription, optionalLayers);
        }

        @PublicAPI(usage = ACCESS)
        public LayerDependencySpecification whereLayer(String name) {
            checkLayerNamesExist(name);
            return new LayerDependencySpecification(name);
        }

        private void checkLayerNamesExist(String... layerNames) {
            for (String layerName : layerNames) {
                checkArgument(layerDefinitions.containLayer(layerName), "There is no layer named '%s'", layerName);
            }
        }

        private static final class LayerDefinitions implements Iterable<LayerDefinition> {
            private final Map<String, LayerDefinition> layerDefinitions = new LinkedHashMap<>();

            void add(LayerDefinition definition) {
                layerDefinitions.put(definition.name, definition);
            }

            boolean containLayer(String layerName) {
                return layerDefinitions.containsKey(layerName);
            }

            DescribedPredicate<JavaClass> containsPredicateFor(String layerName) {
                return containsPredicateFor(singleton(layerName));
            }

            DescribedPredicate<JavaClass> containsPredicateFor(final Collection<String> layerNames) {
                DescribedPredicate<JavaClass> result = alwaysFalse();
                for (LayerDefinition definition : get(layerNames)) {
                    result = result.or(definition.containsPredicate());
                }
                return result;
            }

            private Iterable<LayerDefinition> get(Collection<String> layerNames) {
                Set<LayerDefinition> result = new HashSet<>();
                for (String layerName : layerNames) {
                    result.add(layerDefinitions.get(layerName));
                }
                return result;
            }

            @Override
            public Iterator<LayerDefinition> iterator() {
                return layerDefinitions.values().iterator();
            }
        }

        public final class LayerDefinition {
            private final String name;
            private final boolean optional;
            private DescribedPredicate<JavaClass> containsPredicate;

            private LayerDefinition(String name, boolean optional) {
                checkState(!isNullOrEmpty(name), "Layer name must be present");
                this.name = name;
                this.optional = optional;
            }

            @PublicAPI(usage = ACCESS)
            public LayeredArchitecture definedBy(DescribedPredicate<? super JavaClass> predicate) {
                checkNotNull(predicate, "Supplied predicate must not be null");
                this.containsPredicate = predicate.forSubType();
                return LayeredArchitecture.this.addLayerDefinition(this);
            }

            @PublicAPI(usage = ACCESS)
            public LayeredArchitecture definedBy(String... packageIdentifiers) {
                String description = String.format("'%s'", Joiner.on("', '").join(packageIdentifiers));
                return definedBy(resideInAnyPackage(packageIdentifiers).as(description));
            }

            boolean isOptional() {
                return optional;
            }

            DescribedPredicate<JavaClass> containsPredicate() {
                return containsPredicate;
            }

            @Override
            public String toString() {
                return String.format("%slayer '%s' (%s)", optional ? "optional " : "", name, containsPredicate);
            }
        }

        public final class LayerDependencySpecification {
            private final String layerName;
            private final Set<String> allowedAccessors = new LinkedHashSet<>();
            private String descriptionSuffix;

            private LayerDependencySpecification(String layerName) {
                this.layerName = layerName;
            }

            @PublicAPI(usage = ACCESS)
            public LayeredArchitecture mayNotBeAccessedByAnyLayer() {
                descriptionSuffix = "may not be accessed by any layer";
                return LayeredArchitecture.this.addDependencySpecification(this);
            }

            @PublicAPI(usage = ACCESS)
            public LayeredArchitecture mayOnlyBeAccessedByLayers(String... layerNames) {
                checkLayerNamesExist(layerNames);
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

    @PublicAPI(usage = ACCESS)
    public static OnionArchitecture onionArchitecture() {
        return new OnionArchitecture();
    }

    public static final class OnionArchitecture implements ArchRule {
        private static final String DOMAIN_MODEL_LAYER = "domain model";
        private static final String DOMAIN_SERVICE_LAYER = "domain service";
        private static final String APPLICATION_SERVICE_LAYER = "application service";
        private static final String ADAPTER_LAYER = "adapter";

        private final Optional<String> overriddenDescription;
        private String[] domainModelPackageIdentifiers = new String[0];
        private String[] domainServicePackageIdentifiers = new String[0];
        private String[] applicationPackageIdentifiers = new String[0];
        private Map<String, String[]> adapterPackageIdentifiers = new LinkedHashMap<>();
        private boolean optionalLayers = false;

        private OnionArchitecture() {
            overriddenDescription = Optional.absent();
        }

        private OnionArchitecture(String[] domainModelPackageIdentifiers,
                String[] domainServicePackageIdentifiers,
                String[] applicationPackageIdentifiers,
                Map<String, String[]> adapterPackageIdentifiers,
                Optional<String> overriddenDescription) {
            this.domainModelPackageIdentifiers = domainModelPackageIdentifiers;
            this.domainServicePackageIdentifiers = domainServicePackageIdentifiers;
            this.applicationPackageIdentifiers = applicationPackageIdentifiers;
            this.adapterPackageIdentifiers = adapterPackageIdentifiers;
            this.overriddenDescription = overriddenDescription;
        }

        @PublicAPI(usage = ACCESS)
        public OnionArchitecture domainModels(String... packageIdentifiers) {
            domainModelPackageIdentifiers = packageIdentifiers;
            return this;
        }

        @PublicAPI(usage = ACCESS)
        public OnionArchitecture domainServices(String... packageIdentifiers) {
            domainServicePackageIdentifiers = packageIdentifiers;
            return this;
        }

        @PublicAPI(usage = ACCESS)
        public OnionArchitecture applicationServices(String... packageIdentifiers) {
            applicationPackageIdentifiers = packageIdentifiers;
            return this;
        }

        @PublicAPI(usage = ACCESS)
        public OnionArchitecture adapter(String name, String... packageIdentifiers) {
            adapterPackageIdentifiers.put(name, packageIdentifiers);
            return this;
        }

        @PublicAPI(usage = ACCESS)
        public OnionArchitecture withOptionalLayers(boolean optionalLayers) {
            this.optionalLayers = optionalLayers;
            return this;
        }

        private LayeredArchitecture layeredArchitectureDelegate() {
            LayeredArchitecture layeredArchitectureDelegate = layeredArchitecture()
                    .layer(DOMAIN_MODEL_LAYER).definedBy(domainModelPackageIdentifiers)
                    .layer(DOMAIN_SERVICE_LAYER).definedBy(domainServicePackageIdentifiers)
                    .layer(APPLICATION_SERVICE_LAYER).definedBy(applicationPackageIdentifiers)
                    .layer(ADAPTER_LAYER).definedBy(concatenateAll(adapterPackageIdentifiers.values()))
                    .whereLayer(DOMAIN_MODEL_LAYER).mayOnlyBeAccessedByLayers(DOMAIN_SERVICE_LAYER, APPLICATION_SERVICE_LAYER, ADAPTER_LAYER)
                    .whereLayer(DOMAIN_SERVICE_LAYER).mayOnlyBeAccessedByLayers(APPLICATION_SERVICE_LAYER, ADAPTER_LAYER)
                    .whereLayer(APPLICATION_SERVICE_LAYER).mayOnlyBeAccessedByLayers(ADAPTER_LAYER)
                    .withOptionalLayers(optionalLayers);

            for (Map.Entry<String, String[]> adapter : adapterPackageIdentifiers.entrySet()) {
                String adapterLayer = getAdapterLayer(adapter.getKey());
                layeredArchitectureDelegate = layeredArchitectureDelegate
                        .layer(adapterLayer).definedBy(adapter.getValue())
                        .whereLayer(adapterLayer).mayNotBeAccessedByAnyLayer();
            }
            return layeredArchitectureDelegate.as(getDescription());
        }

        private String[] concatenateAll(Collection<String[]> arrays) {
            List<String> resultList = new ArrayList<>();
            for (String[] array : arrays) {
                resultList.addAll(Arrays.asList(array));
            }
            return resultList.toArray(new String[0]);
        }

        private String getAdapterLayer(String name) {
            return String.format("%s %s", name, ADAPTER_LAYER);
        }

        @Override
        public void check(JavaClasses classes) {
            layeredArchitectureDelegate().check(classes);
        }

        @Override
        public ArchRule because(String reason) {
            return ArchRule.Factory.withBecause(this, reason);
        }

        @Override
        public OnionArchitecture as(String newDescription) {
            return new OnionArchitecture(domainModelPackageIdentifiers, domainServicePackageIdentifiers,
                    applicationPackageIdentifiers, adapterPackageIdentifiers, Optional.of(newDescription));
        }

        @Override
        public EvaluationResult evaluate(JavaClasses classes) {
            return layeredArchitectureDelegate().evaluate(classes);
        }

        @Override
        public String getDescription() {
            if (overriddenDescription.isPresent()) {
                return overriddenDescription.get();
            }

            List<String> lines = newArrayList("Onion architecture consisting of" + (optionalLayers ? " (optional)" : ""));
            if (domainModelPackageIdentifiers.length > 0) {
                lines.add(String.format("domain models ('%s')", Joiner.on("', '").join(domainModelPackageIdentifiers)));
            }
            if (domainServicePackageIdentifiers.length > 0) {
                lines.add(String.format("domain services ('%s')", Joiner.on("', '").join(domainServicePackageIdentifiers)));
            }
            if (applicationPackageIdentifiers.length > 0) {
                lines.add(String.format("application services ('%s')", Joiner.on("', '").join(applicationPackageIdentifiers)));
            }
            for (Map.Entry<String, String[]> adapter : adapterPackageIdentifiers.entrySet()) {
                lines.add(String.format("adapter '%s' ('%s')", adapter.getKey(), Joiner.on("', '").join(adapter.getValue())));
            }
            return Joiner.on(lineSeparator()).join(lines);
        }
    }
}
