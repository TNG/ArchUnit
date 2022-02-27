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
import com.tngtech.archunit.base.PackageMatcher;
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
import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependencyTarget;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.SimpleConditionEvent.violated;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyHaveDependenciesWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyHaveDependentsWhere;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;

/**
 * Offers convenience to assert typical architectures:
 * <ul>
 * <li>{@link #layeredArchitecture()}</li>
 * <li>{@link #onionArchitecture()}</li>
 * </ul>
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
        private final boolean optionalLayers;

        private LayeredArchitecture() {
            this(new LayerDefinitions(),
                    new LinkedHashSet<LayerDependencySpecification>(),
                    new PredicateAggregator<Dependency>().thatORs(),
                    Optional.<String>empty(),
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
            return new LayeredArchitecture(layerDefinitions, dependencySpecifications, irrelevantDependenciesPredicate, overriddenDescription, optionalLayers);
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
                    // we need to set `allowEmptyShould(true)` to allow the layer not empty check to be evaluated. This will provide a nicer error message.
                    .allowEmptyShould(true)
                    .evaluate(classes);
        }

        private EvaluationResult evaluateDependenciesShouldBeSatisfied(JavaClasses classes, LayerDependencySpecification specification) {
            ArchCondition<JavaClass> satisfyLayerDependenciesCondition = specification.constraint == LayerDependencyConstraint.ORIGIN
                    ? onlyHaveDependentsWhere(originMatchesIfDependencyIsRelevant(specification.layerName, specification.allowedLayers))
                    : onlyHaveDependenciesWhere(targetMatchesIfDependencyIsRelevant(specification.layerName, specification.allowedLayers));
            return classes().that(layerDefinitions.containsPredicateFor(specification.layerName))
                    .should(satisfyLayerDependenciesCondition)
                    .allowEmptyShould(true)
                    .evaluate(classes);
        }

        private DescribedPredicate<Dependency> originMatchesIfDependencyIsRelevant(String ownLayer, Set<String> allowedAccessors) {
            DescribedPredicate<Dependency> originPackageMatches =
                    dependencyOrigin(layerDefinitions.containsPredicateFor(allowedAccessors))
                            .or(dependencyOrigin(layerDefinitions.containsPredicateFor(ownLayer)));

            return ifDependencyIsRelevant(originPackageMatches);
        }

        private DescribedPredicate<Dependency> targetMatchesIfDependencyIsRelevant(String ownLayer, Set<String> allowedTargets) {
            DescribedPredicate<Dependency> targetPackageMatches =
                    dependencyTarget(layerDefinitions.containsPredicateFor(allowedTargets))
                            .or(dependencyTarget(layerDefinitions.containsPredicateFor(ownLayer)));

            return ifDependencyIsRelevant(targetPackageMatches);
        }

        private DescribedPredicate<Dependency> ifDependencyIsRelevant(DescribedPredicate<Dependency> originPackageMatches) {
            return irrelevantDependenciesPredicate.isPresent() ?
                    originPackageMatches.or(irrelevantDependenciesPredicate.get()) :
                    originPackageMatches;
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

        /**
         * This method is equivalent to calling {@link #withOptionalLayers(boolean)}, which should be preferred in this context
         * as the meaning is easier to understand.
         */
        @Override
        public ArchRule allowEmptyShould(boolean allowEmptyShould) {
            return withOptionalLayers(allowEmptyShould);
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public LayeredArchitecture as(String newDescription) {
            return new LayeredArchitecture(
                    layerDefinitions, dependencySpecifications,
                    irrelevantDependenciesPredicate, Optional.of(newDescription), optionalLayers);
        }

        /**
         * Configures the rule to ignore any violation from a specific {@code origin} class to a specific {@code target} class.
         * @param origin A {@link Class} object specifying the origin of a {@link Dependency} to ignore
         * @param target A {@link Class} object specifying the target of a {@link Dependency} to ignore
         * @return a {@link LayeredArchitecture} to be used as an {@link ArchRule} or further restricted through a fluent API.
         */
        @PublicAPI(usage = ACCESS)
        public LayeredArchitecture ignoreDependency(Class<?> origin, Class<?> target) {
            return ignoreDependency(equivalentTo(origin), equivalentTo(target));
        }

        /**
         * Same as {@link #ignoreDependency(Class, Class)} but allows specifying origin and target as fully qualified class names.
         */
        @PublicAPI(usage = ACCESS)
        public LayeredArchitecture ignoreDependency(String originFullyQualifiedClassName, String targetFullyQualifiedClassName) {
            return ignoreDependency(name(originFullyQualifiedClassName), name(targetFullyQualifiedClassName));
        }

        /**
         * Same as {@link #ignoreDependency(Class, Class)} but allows specifying origin and target by freely defined predicates.
         * Any dependency where the {@link Dependency#getOriginClass()} matches the {@code origin} predicate
         * and the {@link Dependency#getTargetClass()} matches the {@code target} predicate will be ignored.
         */
        @PublicAPI(usage = ACCESS)
        public LayeredArchitecture ignoreDependency(
                DescribedPredicate<? super JavaClass> origin, DescribedPredicate<? super JavaClass> target) {
            return new LayeredArchitecture(
                    layerDefinitions, dependencySpecifications,
                    irrelevantDependenciesPredicate.add(dependency(origin, target)), overriddenDescription, optionalLayers);
        }

        /**
         * Allows restricting accesses to or from this layer. Note that "access" in the context of a layer refers to
         * any dependency as defined by {@link Dependency}.
         * @param name a layer name as specified before via {@link #layer(String)}
         * @return a specification to fluently define further restrictions
         */
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

            /**
             * Defines a layer by a predicate, i.e. any {@link JavaClass} that will match the predicate will belong to this layer.
             */
            @PublicAPI(usage = ACCESS)
            public LayeredArchitecture definedBy(DescribedPredicate<? super JavaClass> predicate) {
                checkNotNull(predicate, "Supplied predicate must not be null");
                this.containsPredicate = predicate.forSubtype();
                return LayeredArchitecture.this.addLayerDefinition(this);
            }

            /**
             * Defines a layer by package identifiers (compare {@link PackageMatcher})
             */
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

        private enum LayerDependencyConstraint {
            ORIGIN,
            TARGET
        }

        public final class LayerDependencySpecification {
            private final String layerName;
            private final Set<String> allowedLayers = new LinkedHashSet<>();
            private LayerDependencyConstraint constraint;
            private String descriptionSuffix;

            private LayerDependencySpecification(String layerName) {
                this.layerName = layerName;
            }

            /**
             * Forbids any {@link Dependency dependency} from another layer to this layer.
             * @return a {@link LayeredArchitecture} to be used as an {@link ArchRule} or further restricted through a fluent API.
             */
            @PublicAPI(usage = ACCESS)
            public LayeredArchitecture mayNotBeAccessedByAnyLayer() {
                return denyLayerAccess(LayerDependencyConstraint.ORIGIN, "may not be accessed by any layer");
            }

            /**
             * Restricts this layer to only allow the specified layers to have {@link Dependency dependencies} to this layer.
             * @param layerNames the names of other layers (as specified by {@link #layer(String)}) that may access this layer
             * @return a {@link LayeredArchitecture} to be used as an {@link ArchRule} or further restricted through a fluent API.
             */
            @PublicAPI(usage = ACCESS)
            public LayeredArchitecture mayOnlyBeAccessedByLayers(String... layerNames) {
                return restrictLayers(LayerDependencyConstraint.ORIGIN, layerNames, "may only be accessed by layers ['%s']");
            }

            /**
             * Forbids any {@link Dependency dependency} from this layer to any other layer.
             * @return a {@link LayeredArchitecture} to be used as an {@link ArchRule} or further restricted through a fluent API.
             */
            @PublicAPI(usage = ACCESS)
            public LayeredArchitecture mayNotAccessAnyLayer() {
                return denyLayerAccess(LayerDependencyConstraint.TARGET, "may not access any layer");
            }

            /**
             * Restricts this layer to only allow {@link Dependency dependencies} to the specified layers.
             * @param layerNames the only names of other layers (as specified by {@link #layer(String)}) that this layer may access
             * @return a {@link LayeredArchitecture} to be used as an {@link ArchRule} or further restricted through a fluent API.
             */
            @PublicAPI(usage = ACCESS)
            public LayeredArchitecture mayOnlyAccessLayers(String... layerNames) {
                return restrictLayers(LayerDependencyConstraint.TARGET, layerNames, "may only access layers ['%s']");
            }

            private LayeredArchitecture denyLayerAccess(LayerDependencyConstraint constraint, String description) {
                allowedLayers.clear();
                this.constraint = constraint;
                descriptionSuffix = description;
                return LayeredArchitecture.this.addDependencySpecification(this);
            }

            private LayeredArchitecture restrictLayers(LayerDependencyConstraint constraint, String[] layerNames, String descriptionTemplate) {
                checkArgument(layerNames.length > 0, "At least 1 layer name must be provided.");
                checkLayerNamesExist(layerNames);
                allowedLayers.addAll(asList(layerNames));
                this.constraint = constraint;
                descriptionSuffix = String.format(descriptionTemplate, Joiner.on("', '").join(layerNames));
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
        private List<IgnoredDependency> ignoredDependencies = new ArrayList<>();

        private OnionArchitecture() {
            overriddenDescription = Optional.empty();
        }

        private OnionArchitecture(String[] domainModelPackageIdentifiers,
                String[] domainServicePackageIdentifiers,
                String[] applicationPackageIdentifiers,
                Map<String, String[]> adapterPackageIdentifiers,
                List<IgnoredDependency> ignoredDependencies,
                Optional<String> overriddenDescription) {
            this.domainModelPackageIdentifiers = domainModelPackageIdentifiers;
            this.domainServicePackageIdentifiers = domainServicePackageIdentifiers;
            this.applicationPackageIdentifiers = applicationPackageIdentifiers;
            this.adapterPackageIdentifiers = adapterPackageIdentifiers;
            this.ignoredDependencies = ignoredDependencies;
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

        /**
         * @param optionalLayers Whether the different parts of the Onion Architecture (domain models, domain services, ...) should be allowed to be empty.
         *                       If set to {@code false} the {@link OnionArchitecture OnionArchitecture} will fail if any such layer does not contain any class.
         */
        @PublicAPI(usage = ACCESS)
        public OnionArchitecture withOptionalLayers(boolean optionalLayers) {
            this.optionalLayers = optionalLayers;
            return this;
        }

        @PublicAPI(usage = ACCESS)
        public OnionArchitecture ignoreDependency(Class<?> origin, Class<?> target) {
            return ignoreDependency(equivalentTo(origin), equivalentTo(target));
        }

        @PublicAPI(usage = ACCESS)
        public OnionArchitecture ignoreDependency(String origin, String target) {
            return ignoreDependency(name(origin), name(target));
        }

        @PublicAPI(usage = ACCESS)
        public OnionArchitecture ignoreDependency(DescribedPredicate<? super JavaClass> origin, DescribedPredicate<? super JavaClass> target) {
            this.ignoredDependencies.add(new IgnoredDependency(origin, target));
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
            for (IgnoredDependency ignoredDependency : this.ignoredDependencies) {
                layeredArchitectureDelegate = ignoredDependency.ignoreFor(layeredArchitectureDelegate);
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

        /**
         * This method is equivalent to calling {@link #withOptionalLayers(boolean)}, which should be preferred in this context
         * as the meaning is easier to understand.
         */
        @Override
        public ArchRule allowEmptyShould(boolean allowEmptyShould) {
            return withOptionalLayers(allowEmptyShould);
        }

        @Override
        public OnionArchitecture as(String newDescription) {
            return new OnionArchitecture(domainModelPackageIdentifiers, domainServicePackageIdentifiers,
                    applicationPackageIdentifiers, adapterPackageIdentifiers, ignoredDependencies,
                    Optional.of(newDescription));
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

        private static class IgnoredDependency {
            private final DescribedPredicate<? super JavaClass> origin;
            private final DescribedPredicate<? super JavaClass> target;

            IgnoredDependency(DescribedPredicate<? super JavaClass> origin, DescribedPredicate<? super JavaClass> target) {
                this.origin = origin;
                this.target = target;
            }

            LayeredArchitecture ignoreFor(LayeredArchitecture layeredArchitecture) {
                return layeredArchitecture.ignoreDependency(origin, target);
            }
        }
    }
}
