/*
 * Copyright 2019 TNG Technology Consulting GmbH
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
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Optional;
import com.tngtech.archunit.base.PackageMatchers;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.Priority;
import com.tngtech.archunit.lang.syntax.PredicateAggregator;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.collect.Lists.newArrayList;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependency;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependencyOrigin;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyHaveDependentsWhere;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.asList;

/**
 * Offers convenience to assert typical architectures, like a {@link #layeredArchitecture()}.
 */
public final class Architectures {
    private Architectures() {
    }

    /**
     * Can be used to assert a typical layered architecture, e.g. with an UI layer, a business logic layer and
     * a persistence layer, where specific access rules should be adhered to, like UI may not access persistence
     * and each layer may only access lower layers, i.e. UI --&gt; business logic --&gt; persistence.
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
     * 'Persistence' --&gt; 'my.application.somehelper' --&gt; 'Business Logic'<br>
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
        private final Map<String, LayerDefinition> layerDefinitions;
        private final Set<LayerDependencySpecification> dependencySpecifications;
        private final PredicateAggregator<Dependency> irrelevantDependenciesPredicate;
        private final Optional<String> overriddenDescription;

        private LayeredArchitecture() {
            this(new LinkedHashMap<String, LayerDefinition>(),
                    new LinkedHashSet<LayerDependencySpecification>(),
                    new PredicateAggregator<Dependency>().thatORs(),
                    Optional.<String>absent());
        }

        private LayeredArchitecture(Map<String, LayerDefinition> layerDefinitions,
                Set<LayerDependencySpecification> dependencySpecifications,
                PredicateAggregator<Dependency> irrelevantDependenciesPredicate,
                Optional<String> overriddenDescription) {
            this.layerDefinitions = layerDefinitions;
            this.dependencySpecifications = dependencySpecifications;
            this.irrelevantDependenciesPredicate = irrelevantDependenciesPredicate;
            this.overriddenDescription = overriddenDescription;
        }

        private LayeredArchitecture addLayerDefinition(LayerDefinition definition) {
            layerDefinitions.put(definition.name, definition);
            return this;
        }

        private LayeredArchitecture addDependencySpecification(LayerDependencySpecification dependencySpecification) {
            dependencySpecifications.add(dependencySpecification);
            return this;
        }

        @PublicAPI(usage = ACCESS)
        public LayerDefinition layer(String name) {
            return new LayerDefinition(name);
        }

        @Override
        @PublicAPI(usage = ACCESS)
        public String getDescription() {
            if (overriddenDescription.isPresent()) {
                return overriddenDescription.get();
            }

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
        @PublicAPI(usage = ACCESS)
        public EvaluationResult evaluate(JavaClasses classes) {
            EvaluationResult result = new EvaluationResult(this, Priority.MEDIUM);
            for (LayerDependencySpecification specification : dependencySpecifications) {
                SortedSet<String> packagesOfOwnLayer = packagesOf(specification.layerName);
                SortedSet<String> packagesOfAllowedAccessors = packagesOf(specification.allowedAccessors);
                packagesOfAllowedAccessors.addAll(packagesOfOwnLayer);

                EvaluationResult partial = classes().that().resideInAnyPackage(toArray(packagesOfOwnLayer))
                        .should(onlyHaveDependentsWhere(originPackageMatchesIfDependencyIsRelevant(packagesOfAllowedAccessors)))
                        .evaluate(classes);

                result.add(partial);
            }
            return result;
        }

        private DescribedPredicate<Dependency> originPackageMatchesIfDependencyIsRelevant(SortedSet<String> packagesOfAllowedAccessors) {
            DescribedPredicate<Dependency> originPackageMatches =
                    dependencyOrigin(JavaClass.Functions.GET_PACKAGE_NAME.is(PackageMatchers.of(toArray(packagesOfAllowedAccessors))));

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

        @Override
        @PublicAPI(usage = ACCESS)
        public LayeredArchitecture as(String newDescription) {
            return new LayeredArchitecture(
                    layerDefinitions, dependencySpecifications,
                    irrelevantDependenciesPredicate, Optional.of(newDescription));
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
                    irrelevantDependenciesPredicate.add(dependency(origin, target)), overriddenDescription);
        }

        private String[] toArray(Set<String> strings) {
            return strings.toArray(new String[0]);
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

        @PublicAPI(usage = ACCESS)
        public LayerDependencySpecification whereLayer(String name) {
            checkLayersExist(name);
            return new LayerDependencySpecification(name);
        }

        private void checkLayersExist(String... layerNames) {
            for (String layerName : layerNames) {
                checkArgument(layerDefinitions.containsKey(layerName), "There is no layer named '%s'", layerName);
            }
        }

        public final class LayerDefinition {
            private final String name;
            private Set<String> packageIdentifiers;

            private LayerDefinition(String name) {
                checkState(!isNullOrEmpty(name), "Layer name must be present");
                this.name = name;
            }

            @PublicAPI(usage = ACCESS)
            public LayeredArchitecture definedBy(String... packageIdentifiers) {
                this.packageIdentifiers = ImmutableSet.copyOf(packageIdentifiers);
                return LayeredArchitecture.this.addLayerDefinition(this);
            }

            @Override
            public String toString() {
                return String.format("layer '%s' ('%s')", name, Joiner.on("', '").join(packageIdentifiers));
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
                checkLayersExist(layerNames);
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
        private static final String APPLICATION_LAYER = "application";
        private static final String ADAPTER_LAYER = "adapter";

        private String[] domainModelPackageIdentifiers = new String[0];
        private String[] domainServicePackageIdentifiers = new String[0];
        private String[] applicationPackageIdentifiers = new String[0];
        private Map<String, String[]> adapterPackageIdentifiers = new LinkedHashMap<>();

        private final Optional<String> overriddenDescription;

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
        public OnionArchitecture domainModel(String... packageIdentifiers) {
            domainModelPackageIdentifiers = packageIdentifiers;
            return this;
        }

        @PublicAPI(usage = ACCESS)
        public OnionArchitecture domainService(String... packageIdentifiers) {
            domainServicePackageIdentifiers = packageIdentifiers;
            return this;
        }

        @PublicAPI(usage = ACCESS)
        public OnionArchitecture application(String... packageIdentifiers) {
            applicationPackageIdentifiers = packageIdentifiers;
            return this;
        }

        @PublicAPI(usage = ACCESS)
        public OnionArchitecture adapter(String name, String... packageIdentifiers) {
            adapterPackageIdentifiers.put(name, packageIdentifiers);
            return this;
        }

        private LayeredArchitecture layeredArchitectureDelegate() {
            LayeredArchitecture layeredArchitectureDelegate = layeredArchitecture()
                    .layer(DOMAIN_MODEL_LAYER).definedBy(domainModelPackageIdentifiers)
                    .layer(DOMAIN_SERVICE_LAYER).definedBy(domainServicePackageIdentifiers)
                    .layer(APPLICATION_LAYER).definedBy(applicationPackageIdentifiers)
                    .layer(ADAPTER_LAYER).definedBy(concatenateAll(adapterPackageIdentifiers.values()));
            for (Map.Entry<String, String[]> adapter : adapterPackageIdentifiers.entrySet()) {
                layeredArchitectureDelegate = layeredArchitectureDelegate
                        .layer(getAdapterLayer(adapter.getKey())).definedBy(adapter.getValue());
            }
            layeredArchitectureDelegate = layeredArchitectureDelegate
                    .whereLayer(DOMAIN_MODEL_LAYER).mayOnlyBeAccessedByLayers(DOMAIN_SERVICE_LAYER, APPLICATION_LAYER, ADAPTER_LAYER)
                    .whereLayer(DOMAIN_SERVICE_LAYER).mayOnlyBeAccessedByLayers(APPLICATION_LAYER, ADAPTER_LAYER)
                    .whereLayer(APPLICATION_LAYER).mayOnlyBeAccessedByLayers(ADAPTER_LAYER);
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

            List<String> lines = newArrayList("Onion architecture consisting of");
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
