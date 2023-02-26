/*
 * Copyright 2014-2023 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.library.dependencies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.SortedSetMultimap;
import com.tngtech.archunit.base.DescribedPredicate;
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
import static com.tngtech.archunit.library.cycle_detection.CycleConfiguration.MAX_NUMBER_OF_CYCLES_TO_DETECT_PROPERTY_NAME;
import static com.tngtech.archunit.library.dependencies.CycleRuleConfiguration.MAX_NUMBER_OF_DEPENDENCIES_TO_SHOW_PER_EDGE_PROPERTY_NAME;
import static java.lang.System.lineSeparator;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

class SliceCycleArchCondition extends ArchCondition<Slice> {
    private static final Logger log = LoggerFactory.getLogger(SliceCycleArchCondition.class);

    private final DescribedPredicate<Dependency> predicate;
    private ClassesToSlicesMapping classesToSlicesMapping;
    private SliceCycleDetector cycleDetector;
    private EventRecorder eventRecorder;

    SliceCycleArchCondition(DescribedPredicate<Dependency> predicate) {
        super("be free of cycles");
        this.predicate = predicate;
    }

    @Override
    public void init(Collection<Slice> allSlices) {
        classesToSlicesMapping = new ClassesToSlicesMapping(allSlices);
        cycleDetector = new SliceCycleDetector(allSlices);
        eventRecorder = new EventRecorder();
    }

    @Override
    public void check(Slice slice, ConditionEvents events) {
        cycleDetector.addEdges(createSliceDependencies(slice, classesToSlicesMapping, predicate));
    }

    private static Set<SliceDependency> createSliceDependencies(Slice slice, ClassesToSlicesMapping classesToSlicesMapping, DescribedPredicate<Dependency> predicate) {
        SortedSetMultimap<Slice, Dependency> targetSlicesWithDependencies = targetsOf(slice, classesToSlicesMapping, predicate);
        return sortedEntries(targetSlicesWithDependencies).stream()
                .map(entry -> new SliceDependency(slice, entry.getKey(), entry.getValue()))
                .collect(toImmutableSet());
    }

    private static SortedSetMultimap<Slice, Dependency> targetsOf(Slice slice,
            ClassesToSlicesMapping classesToSlicesMapping, DescribedPredicate<Dependency> predicate) {

        SortedSetMultimap<Slice, Dependency> result = hashKeys().treeSetValues().build();
        slice.getDependenciesFromSelf().stream()
                .filter(predicate)
                .filter(dependency -> classesToSlicesMapping.containsKey(dependency.getTargetClass()))
                .forEach(dependency -> result.put(classesToSlicesMapping.get(dependency.getTargetClass()), dependency));
        return result;
    }

    // unfortunately SortedSetMultimap has no good API to iterate over all SortedSet values :-(
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Set<Map.Entry<Slice, SortedSet<Dependency>>> sortedEntries(SortedSetMultimap<Slice, Dependency> multimap) {
        return (Set) multimap.asMap().entrySet();
    }

    @Override
    public void finish(ConditionEvents events) {
        Cycles<SliceDependency> cycles = cycleDetector.findCycles();
        if (cycles.maxNumberOfCyclesReached()) {
            events.setInformationAboutNumberOfViolations(String.format(
                    " >= %d times - the maximum number of cycles to detect has been reached; "
                            + "this limit can be adapted using the `archunit.properties` value `%s=xxx`",
                    cycles.size(), MAX_NUMBER_OF_CYCLES_TO_DETECT_PROPERTY_NAME));
        }
        for (Cycle<SliceDependency> cycle : cycles) {
            eventRecorder.record(cycle, events);
        }
        releaseResources();
    }

    private void releaseResources() {
        classesToSlicesMapping = null;
        cycleDetector = null;
        eventRecorder = null;
    }

    private static class ClassesToSlicesMapping {
        private final Iterable<Slice> allSlices;
        private Map<JavaClass, Slice> mapping;

        private ClassesToSlicesMapping(Iterable<Slice> allSlices) {
            this.allSlices = allSlices;
        }

        public Slice get(JavaClass javaClass) {
            return mapping().get(javaClass);
        }

        private Map<JavaClass, Slice> mapping() {
            if (mapping != null) {
                return mapping;
            }
            ImmutableMap.Builder<JavaClass, Slice> result = ImmutableMap.builder();
            for (Slice slice : allSlices) {
                for (JavaClass javaClass : slice) {
                    result.put(javaClass, slice);
                }
            }
            return mapping = result.build();
        }

        public boolean containsKey(JavaClass javaClass) {
            return mapping().containsKey(javaClass);
        }
    }

    private static class SliceCycleDetector {
        private final Collection<Slice> slices;
        private final Set<SliceDependency> sliceDependencies = new HashSet<>();

        SliceCycleDetector(Collection<Slice> slices) {
            this.slices = checkNotNull(slices);
        }

        void addEdges(Collection<SliceDependency> sliceDependencies) {
            this.sliceDependencies.addAll(sliceDependencies);
        }

        Cycles<SliceDependency> findCycles() {
            return CycleDetector.detectCycles(slices, sliceDependencies);
        }
    }

    private static class SliceDependency implements Edge<Slice> {
        private final Slice origin;
        private final Slice target;
        private final SortedSet<Dependency> classDependencies;

        private SliceDependency(Slice origin, Slice target, SortedSet<Dependency> classDependencies) {
            this.origin = origin;
            this.target = target;
            this.classDependencies = classDependencies;
        }

        @Override
        public Slice getOrigin() {
            return origin;
        }

        @Override
        public Slice getTarget() {
            return target;
        }

        SortedSet<Dependency> toClassDependencies() {
            return classDependencies;
        }
    }

    private static class EventRecorder {
        private static final String CYCLE_DETECTED_SECTION_INTRO = "Cycle detected: ";
        private static final String DEPENDENCY_DETAILS_INDENT = Strings.repeat(" ", 4);

        private final CycleRuleConfiguration cycleConfiguration = new CycleRuleConfiguration();

        private EventRecorder() {
            log.trace("Maximum number of dependencies to report per edge is set to {}; "
                            + "this limit can be adapted using the `archunit.properties` value `{}=xxx`",
                    cycleConfiguration.getMaxNumberOfDependenciesToShowPerEdge(), MAX_NUMBER_OF_DEPENDENCIES_TO_SHOW_PER_EDGE_PROPERTY_NAME);
        }

        void record(Cycle<SliceDependency> cycle, ConditionEvents events) {
            events.add(newEvent(cycle));
        }

        private ConditionEvent newEvent(Cycle<SliceDependency> cycle) {
            Map<String, SliceDependency> descriptionsToEdges = sortEdgesByDescription(cycle);
            String description = createDescription(descriptionsToEdges.keySet(), CYCLE_DETECTED_SECTION_INTRO.length());
            String details = createDetails(descriptionsToEdges);
            return new SimpleConditionEvent(cycle,
                    false,
                    CYCLE_DETECTED_SECTION_INTRO + description + lineSeparator() + details);
        }

        private Map<String, SliceDependency> sortEdgesByDescription(Cycle<SliceDependency> cycle) {
            LinkedList<SliceDependency> edges = new LinkedList<>(cycle.getEdges());
            SliceDependency startEdge = findStartEdge(cycle);
            while (!edges.getFirst().equals(startEdge)) {
                edges.addLast(edges.pollFirst());
            }
            Map<String, SliceDependency> descriptionToEdge = new LinkedHashMap<>();
            for (SliceDependency edge : edges) {
                descriptionToEdge.put(edge.getOrigin().getDescription(), edge);
            }
            return descriptionToEdge;
        }

        // A cycle always has edges, so we know that there is always at least one edge and by that a minimum element
        // with respect to comparing the description lexicographically
        @SuppressWarnings("OptionalGetWithoutIsPresent")
        private static SliceDependency findStartEdge(Cycle<SliceDependency> cycle) {
            return cycle.getEdges().stream().min(comparing(input -> input.getOrigin().getDescription())).get();
        }

        private String createDescription(Collection<String> edgeDescriptions, int indent) {
            List<String> descriptions = new ArrayList<>(edgeDescriptions);
            descriptions.add(descriptions.get(0));
            return Joiner.on(" -> " + lineSeparator() + Strings.repeat(" ", indent)).join(descriptions);
        }

        private String createDetails(Map<String, SliceDependency> descriptionsToEdges) {
            List<String> details = new ArrayList<>();
            int sliceIndex = 0;
            for (Map.Entry<String, SliceDependency> edgeWithDescription : descriptionsToEdges.entrySet()) {
                ++sliceIndex;
                details.add(String.format("  %d. Dependencies of %s", sliceIndex, edgeWithDescription.getKey()));
                details.addAll(dependenciesDescription(edgeWithDescription.getValue()));
            }
            return Joiner.on(lineSeparator()).join(details);
        }

        private List<String> dependenciesDescription(SliceDependency edge) {
            int maxDependencies = cycleConfiguration.getMaxNumberOfDependenciesToShowPerEdge();
            Collection<Dependency> allDependencies = edge.toClassDependencies();
            boolean tooManyDependenciesToDisplay = allDependencies.size() > maxDependencies;

            Collection<Dependency> dependenciesToDisplay = tooManyDependenciesToDisplay
                    ? allDependencies.stream().limit(maxDependencies).collect(toList())
                    : allDependencies;

            List<String> result = dependenciesToDisplay.stream()
                    .map(dependency -> DEPENDENCY_DETAILS_INDENT + "- " + dependency.getDescription())
                    .collect(toCollection(ArrayList::new));
            if (tooManyDependenciesToDisplay) {
                result.add(DEPENDENCY_DETAILS_INDENT + String.format("(%d further dependencies have been omitted...)",
                        allDependencies.size() - dependenciesToDisplay.size()));
            }
            return result;
        }
    }
}
