/*
 * Copyright 2014-2026 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.library.cycle_detection.rules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SortedSetMultimap;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.core.Convertible;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.cycle_detection.Cycle;
import com.tngtech.archunit.library.cycle_detection.CycleDetector;
import com.tngtech.archunit.library.cycle_detection.Cycles;
import com.tngtech.archunit.library.cycle_detection.Edge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableSet.toImmutableSet;
import static com.google.common.collect.MultimapBuilder.hashKeys;
import static com.tngtech.archunit.PublicAPI.State.EXPERIMENTAL;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;
import static com.tngtech.archunit.library.cycle_detection.CycleConfiguration.MAX_NUMBER_OF_CYCLES_TO_DETECT_PROPERTY_NAME;
import static com.tngtech.archunit.library.cycle_detection.rules.CycleRuleConfiguration.MAX_NUMBER_OF_DEPENDENCIES_TO_SHOW_PER_EDGE_PROPERTY_NAME;
import static java.lang.System.lineSeparator;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;

/**
 * A generic {@link ArchCondition} to check arbitrary {@code COMPONENT}s consisting of {@link JavaClass JavaClasses}
 * for cyclic dependencies between those components (induced by the {@link Dependency dependencies} of the contained {@link JavaClass classes}).<br>
 * Construct it by following the fluent interface of {@link #builder()}.
 *
 * @param <COMPONENT> The type of the component to check dependencies between
 */
@PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
public final class CycleArchCondition<COMPONENT> extends ArchCondition<COMPONENT> {
    private static final Logger log = LoggerFactory.getLogger(CycleArchCondition.class);

    private final Function<COMPONENT, Iterable<JavaClass>> getClasses;
    private final Function<COMPONENT, String> getDescription;
    private final Function<COMPONENT, Set<Dependency>> getOutgoingDependencies;
    private final Predicate<Dependency> relevantClassDependenciesPredicate;
    private ClassesToComponentsMapping<COMPONENT> classesToComponentsMapping;
    private ComponentCycleDetector<COMPONENT> cycleDetector;
    private EventRecorder<COMPONENT> eventRecorder;

    @SuppressWarnings({"unchecked", "rawtypes"}) // Function is contra-variant in its input parameter
    private CycleArchCondition(
            Function<? super COMPONENT, Iterable<JavaClass>> retrieveClasses,
            Function<? super COMPONENT, String> retrieveDescription,
            Function<? super COMPONENT, Set<Dependency>> retrieveOutgoingDependencies,
            Predicate<? super Dependency> relevantClassDependenciesPredicate) {
        super("be free of cycles");
        this.getClasses = (Function) retrieveClasses;
        this.getDescription = (Function) retrieveDescription;
        this.getOutgoingDependencies = (Function) retrieveOutgoingDependencies;
        this.relevantClassDependenciesPredicate = (Predicate) relevantClassDependenciesPredicate;
    }

    @Override
    public void init(Collection<COMPONENT> allComponents) {
        classesToComponentsMapping = new ClassesToComponentsMapping<>(allComponents, getClasses);
        cycleDetector = new ComponentCycleDetector<>(allComponents);
        eventRecorder = new EventRecorder<>(getDescription);
    }

    @Override
    public void check(COMPONENT component, ConditionEvents events) {
        cycleDetector.addEdges(createComponentDependencies(component));
    }

    private Set<ComponentDependency<COMPONENT>> createComponentDependencies(COMPONENT component) {
        SortedSetMultimap<COMPONENT, Dependency> targetComponentsWithDependencies = targetsOf(component);
        return sortedEntries(targetComponentsWithDependencies).stream()
                .map(entry -> new ComponentDependency<>(component, entry.getKey(), entry.getValue()))
                .collect(toImmutableSet());
    }

    private SortedSetMultimap<COMPONENT, Dependency> targetsOf(COMPONENT component) {
        SortedSetMultimap<COMPONENT, Dependency> result = hashKeys().treeSetValues().build();
        getOutgoingDependencies.apply(component).stream()
                .filter(relevantClassDependenciesPredicate)
                .filter(dependency -> classesToComponentsMapping.containsKey(dependency.getTargetClass()))
                .forEach(dependency -> result.put(classesToComponentsMapping.get(dependency.getTargetClass()), dependency));
        return result;
    }

    // unfortunately SortedSetMultimap has no good API to iterate over all SortedSet values :-(
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Set<Map.Entry<COMPONENT, SortedSet<Dependency>>> sortedEntries(SortedSetMultimap<COMPONENT, Dependency> multimap) {
        return (Set) multimap.asMap().entrySet();
    }

    @Override
    public void finish(ConditionEvents events) {
        Cycles<ComponentDependency<COMPONENT>> cycles = cycleDetector.findCycles();
        if (cycles.maxNumberOfCyclesReached()) {
            events.setInformationAboutNumberOfViolations(String.format(
                    " >= %d times - the maximum number of cycles to detect has been reached; "
                            + "this limit can be adapted using the `archunit.properties` value `%s=xxx`",
                    cycles.size(), MAX_NUMBER_OF_CYCLES_TO_DETECT_PROPERTY_NAME));
        }
        for (Cycle<ComponentDependency<COMPONENT>> cycle : cycles) {
            eventRecorder.record(cycle, events);
        }
        releaseResources();
    }

    private void releaseResources() {
        classesToComponentsMapping = null;
        cycleDetector = null;
        eventRecorder = null;
    }

    private static class ClassesToComponentsMapping<COMPONENT> {
        private final Iterable<COMPONENT> allComponents;
        private final Function<COMPONENT, Iterable<JavaClass>> getClassesOfComponent;
        private Map<JavaClass, COMPONENT> mapping;

        private ClassesToComponentsMapping(Iterable<COMPONENT> allComponents, Function<COMPONENT, Iterable<JavaClass>> getClassesOfComponent) {
            this.allComponents = allComponents;
            this.getClassesOfComponent = getClassesOfComponent;
        }

        public COMPONENT get(JavaClass javaClass) {
            return mapping().get(javaClass);
        }

        private Map<JavaClass, COMPONENT> mapping() {
            if (mapping != null) {
                return mapping;
            }
            ImmutableMap.Builder<JavaClass, COMPONENT> result = ImmutableMap.builder();
            for (COMPONENT component : allComponents) {
                for (JavaClass javaClass : getClassesOfComponent.apply(component)) {
                    result.put(javaClass, component);
                }
            }
            return mapping = result.build();
        }

        public boolean containsKey(JavaClass javaClass) {
            return mapping().containsKey(javaClass);
        }
    }

    private static class ComponentCycleDetector<COMPONENT> {
        private final Collection<COMPONENT> components;
        private final Set<ComponentDependency<COMPONENT>> componentDependencies = new HashSet<>();

        ComponentCycleDetector(Collection<COMPONENT> components) {
            this.components = checkNotNull(components);
        }

        void addEdges(Collection<ComponentDependency<COMPONENT>> componentDependencies) {
            this.componentDependencies.addAll(componentDependencies);
        }

        Cycles<ComponentDependency<COMPONENT>> findCycles() {
            return CycleDetector.detectCycles(components, componentDependencies);
        }
    }

    private static class ComponentDependency<COMPONENT> implements Edge<COMPONENT>, Convertible {
        private final COMPONENT origin;
        private final COMPONENT target;
        private final SortedSet<Dependency> classDependencies;

        private ComponentDependency(COMPONENT origin, COMPONENT target, SortedSet<Dependency> classDependencies) {
            this.origin = origin;
            this.target = target;
            this.classDependencies = classDependencies;
        }

        @Override
        public COMPONENT getOrigin() {
            return origin;
        }

        @Override
        public COMPONENT getTarget() {
            return target;
        }

        SortedSet<Dependency> toClassDependencies() {
            return classDependencies;
        }

        @Override
        @SuppressWarnings("unchecked") // compatibility is explicitly checked
        public <T> Set<T> convertTo(Class<T> type) {
            if (type.isAssignableFrom(getClass())) {
                return (Set<T>) singleton(this);
            }
            return toClassDependencies().stream().flatMap(it -> it.convertTo(type).stream()).collect(toSet());
        }
    }

    private static class EventRecorder<COMPONENT> {
        private static final String CYCLE_DETECTED_SECTION_INTRO = "Cycle detected: ";
        private static final String CYCLE_EDGE_DESCRIPTION_SEPARATOR = " -> " + lineSeparator() + Strings.repeat(" ", CYCLE_DETECTED_SECTION_INTRO.length());
        private static final String DEPENDENCY_DETAILS_INDENT = Strings.repeat(" ", 4);

        private final CycleRuleConfiguration cycleConfiguration = new CycleRuleConfiguration();
        private final Function<COMPONENT, String> getDescriptionOfComponent;

        private EventRecorder(Function<COMPONENT, String> getDescriptionOfComponent) {
            this.getDescriptionOfComponent = getDescriptionOfComponent;
            log.trace("Maximum number of dependencies to report per edge is set to {}; "
                            + "this limit can be adapted using the `archunit.properties` value `{}=xxx`",
                    cycleConfiguration.getMaxNumberOfDependenciesToShowPerEdge(), MAX_NUMBER_OF_DEPENDENCIES_TO_SHOW_PER_EDGE_PROPERTY_NAME);
        }

        void record(Cycle<ComponentDependency<COMPONENT>> cycle, ConditionEvents events) {
            events.add(newEvent(cycle));
        }

        private ConditionEvent newEvent(Cycle<ComponentDependency<COMPONENT>> cycle) {
            Map<String, ComponentDependency<COMPONENT>> descriptionsToEdges = sortEdgesByDescription(cycle);
            String description = createDescription(descriptionsToEdges.keySet());
            String details = createDetails(descriptionsToEdges);
            return SimpleConditionEvent.violated(cycle, CYCLE_DETECTED_SECTION_INTRO + description + lineSeparator() + details);
        }

        private Map<String, ComponentDependency<COMPONENT>> sortEdgesByDescription(Cycle<ComponentDependency<COMPONENT>> cycle) {
            LinkedList<ComponentDependency<COMPONENT>> edges = new LinkedList<>(cycle.getEdges());
            ComponentDependency<COMPONENT> startEdge = findStartEdge(cycle);
            while (!edges.getFirst().equals(startEdge)) {
                edges.addLast(edges.pollFirst());
            }
            Map<String, ComponentDependency<COMPONENT>> descriptionToEdge = new LinkedHashMap<>();
            for (ComponentDependency<COMPONENT> edge : edges) {
                descriptionToEdge.put(getDescriptionOfComponent.apply(edge.getOrigin()), edge);
            }
            return descriptionToEdge;
        }

        // A cycle always has edges, so we know that there is always at least one edge and by that a minimum element
        // with respect to comparing the description lexicographically
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        private ComponentDependency<COMPONENT> findStartEdge(Cycle<ComponentDependency<COMPONENT>> cycle) {
            return cycle.getEdges().stream().min(comparing(input -> getDescriptionOfComponent.apply(input.getOrigin()))).get();
        }

        private String createDescription(Collection<String> edgeDescriptions) {
            List<String> descriptions = new ArrayList<>(edgeDescriptions);
            descriptions.add(descriptions.get(0));
            return Joiner.on(CYCLE_EDGE_DESCRIPTION_SEPARATOR).join(descriptions);
        }

        private String createDetails(Map<String, ComponentDependency<COMPONENT>> descriptionsToEdges) {
            List<String> details = new ArrayList<>();
            AtomicInteger componentIndex = new AtomicInteger(0);
            descriptionsToEdges.forEach((description, dependencies) -> {
                details.add(String.format("  %d. Dependencies of %s", componentIndex.incrementAndGet(), description));
                details.addAll(dependenciesDescription(dependencies));
            });
            return Joiner.on(lineSeparator()).join(details);
        }

        private List<String> dependenciesDescription(ComponentDependency<COMPONENT> edge) {
            int maxDependencies = cycleConfiguration.getMaxNumberOfDependenciesToShowPerEdge();
            Collection<Dependency> allDependencies = edge.toClassDependencies();
            boolean tooManyDependenciesToDisplay = allDependencies.size() > maxDependencies;

            List<String> result = allDependencies.stream()
                    .limit(maxDependencies)
                    .map(dependency -> DEPENDENCY_DETAILS_INDENT + "- " + dependency.getDescription())
                    .collect(toCollection(ArrayList::new));
            if (tooManyDependenciesToDisplay) {
                result.add(DEPENDENCY_DETAILS_INDENT + String.format("(%d further dependencies have been omitted...)",
                        allDependencies.size() - maxDependencies));
            }
            return result;
        }
    }

    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public static <COMPONENT> NeedsRetrieveClasses<COMPONENT> builder() {
        return new Builder<>();
    }

    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public static final class Builder<COMPONENT> implements NeedsRetrieveClasses<COMPONENT>, NeedsRetrieveDescription<COMPONENT>, NeedsRetrieveOutgoingDependencies<COMPONENT> {
        private Function<? super COMPONENT, Iterable<JavaClass>> retrieveClasses;
        private Function<? super COMPONENT, String> retrieveDescription;
        private Function<? super COMPONENT, Set<Dependency>> retrieveOutgoingDependencies;
        private Predicate<? super Dependency> relevantClassDependenciesPredicate = __ -> true;

        private Builder() {
        }

        @Override
        public NeedsRetrieveDescription<COMPONENT> retrieveClassesBy(Function<? super COMPONENT, Iterable<JavaClass>> retrieveClasses) {
            this.retrieveClasses = checkNotNull(retrieveClasses);
            return this;
        }

        @Override
        public NeedsRetrieveOutgoingDependencies<COMPONENT> retrieveDescriptionBy(Function<? super COMPONENT, String> retrieveDescription) {
            this.retrieveDescription = checkNotNull(retrieveDescription);
            return this;
        }

        @Override
        public Builder<COMPONENT> retrieveOutgoingDependenciesBy(Function<? super COMPONENT, Set<Dependency>> retrieveOutgoingDependencies) {
            this.retrieveOutgoingDependencies = checkNotNull(retrieveOutgoingDependencies);
            return this;
        }

        /**
         * @param relevantClassDependenciesPredicate A {@link Predicate} to decide which {@link Dependency dependencies} are relevant when checking for cycles
         */
        @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
        public Builder<COMPONENT> onlyConsiderDependencies(Predicate<? super Dependency> relevantClassDependenciesPredicate) {
            this.relevantClassDependenciesPredicate = checkNotNull(relevantClassDependenciesPredicate);
            return this;
        }

        /**
         * @return A new {@link CycleArchCondition}
         */
        @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
        public CycleArchCondition<COMPONENT> build() {
            return new CycleArchCondition<>(retrieveClasses, retrieveDescription, retrieveOutgoingDependencies, relevantClassDependenciesPredicate);
        }
    }

    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public interface NeedsRetrieveClasses<COMPONENT> {
        /**
         * @param retrieveClasses A {@link Function} to retrieve the contained {@link JavaClass classes} for any given {@code COMPONENT}
         */
        @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
        NeedsRetrieveDescription<COMPONENT> retrieveClassesBy(Function<? super COMPONENT, Iterable<JavaClass>> retrieveClasses);
    }

    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public interface NeedsRetrieveDescription<COMPONENT> {
        /**
         * @param retrieveDescription A {@link Function} to retrieve the description of a {@code COMPONENT}
         */
        @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
        NeedsRetrieveOutgoingDependencies<COMPONENT> retrieveDescriptionBy(Function<? super COMPONENT, String> retrieveDescription);
    }

    @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
    public interface NeedsRetrieveOutgoingDependencies<COMPONENT> {
        /**
         * @param retrieveOutgoingDependencies A {@link Function} to retrieve the outgoing {@link Dependency dependencies} of a {@code COMPONENT}
         */
        @PublicAPI(usage = ACCESS, state = EXPERIMENTAL)
        Builder<COMPONENT> retrieveOutgoingDependenciesBy(Function<? super COMPONENT, Set<Dependency>> retrieveOutgoingDependencies);
    }
}
