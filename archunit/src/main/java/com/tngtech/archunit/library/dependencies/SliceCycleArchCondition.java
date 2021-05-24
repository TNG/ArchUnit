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
package com.tngtech.archunit.library.dependencies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ForwardingSet;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Ordering;
import com.google.common.collect.SortedSetMultimap;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Guava;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.collect.MultimapBuilder.hashKeys;
import static com.tngtech.archunit.library.dependencies.CycleConfiguration.MAX_NUMBER_OF_CYCLES_TO_DETECT_PROPERTY_NAME;
import static com.tngtech.archunit.library.dependencies.CycleConfiguration.MAX_NUMBER_OF_DEPENDENCIES_TO_SHOW_PER_EDGE_PROPERTY_NAME;
import static java.lang.System.lineSeparator;

class SliceCycleArchCondition extends ArchCondition<Slice> {
    private static final Logger log = LoggerFactory.getLogger(SliceCycleArchCondition.class);

    private final DescribedPredicate<Dependency> predicate;
    private ClassesToSlicesMapping classesToSlicesMapping;
    private Graph<Slice, Dependency> graph;
    private EventRecorder eventRecorder;

    SliceCycleArchCondition(DescribedPredicate<Dependency> predicate) {
        super("be free of cycles");
        this.predicate = predicate;
    }

    @Override
    public void init(Iterable<Slice> allSlices) {
        initializeResources(allSlices);
        graph.addNodes(allSlices);
    }

    private void initializeResources(Iterable<Slice> allSlices) {
        classesToSlicesMapping = new ClassesToSlicesMapping(allSlices);
        graph = new Graph<>();
        eventRecorder = new EventRecorder();
    }

    @Override
    public void check(Slice slice, ConditionEvents events) {
        graph.addEdges(SliceDependencies.of(slice, classesToSlicesMapping, predicate));
    }

    @Override
    public void finish(ConditionEvents events) {
        Graph.Cycles<Slice, Dependency> cycles = graph.findCycles();
        if (cycles.maxNumberOfCyclesReached()) {
            events.setInformationAboutNumberOfViolations(String.format(
                    " >= %d times - the maximum number of cycles to detect has been reached; "
                            + "this limit can be adapted using the `archunit.properties` value `%s=xxx`",
                    cycles.size(), MAX_NUMBER_OF_CYCLES_TO_DETECT_PROPERTY_NAME));
        }
        for (Cycle<Slice, Dependency> cycle : cycles) {
            eventRecorder.record(cycle, events);
        }
        releaseResources();
    }

    private void releaseResources() {
        classesToSlicesMapping = null;
        graph = null;
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

    private static class SliceDependencies extends ForwardingSet<Edge<Slice, Dependency>> {
        private final Set<Edge<Slice, Dependency>> edges;

        private SliceDependencies(Slice slice, ClassesToSlicesMapping classesToSlicesMapping, DescribedPredicate<Dependency> predicate) {
            SortedSetMultimap<Slice, Dependency> targetSlicesWithDependencies = targetsOf(slice, classesToSlicesMapping, predicate);
            ImmutableSet.Builder<Edge<Slice, Dependency>> edgeBuilder = ImmutableSet.builder();
            for (Map.Entry<Slice, SortedSet<Dependency>> entry : sortedEntries(targetSlicesWithDependencies)) {
                edgeBuilder.add(new Edge<>(slice, entry.getKey(), entry.getValue()));
            }
            this.edges = edgeBuilder.build();
        }

        private SortedSetMultimap<Slice, Dependency> targetsOf(Slice slice,
                ClassesToSlicesMapping classesToSlicesMapping, DescribedPredicate<Dependency> predicate) {
            SortedSetMultimap<Slice, Dependency> result = hashKeys().treeSetValues().build();
            for (Dependency dependency : Guava.Iterables.filter(slice.getDependenciesFromSelf(), predicate)) {
                if (classesToSlicesMapping.containsKey(dependency.getTargetClass())) {
                    result.put(classesToSlicesMapping.get(dependency.getTargetClass()), dependency);
                }
            }
            return result;
        }

        // unfortunately SortedSetMultimap has no good API to iterate over all SortedSet values :-(
        @SuppressWarnings({"unchecked", "rawtypes"})
        private Set<Map.Entry<Slice, SortedSet<Dependency>>> sortedEntries(SortedSetMultimap<Slice, Dependency> multimap) {
            return (Set) multimap.asMap().entrySet();
        }

        @Override
        protected Set<Edge<Slice, Dependency>> delegate() {
            return edges;
        }

        static SliceDependencies of(Slice slice, ClassesToSlicesMapping classesToSlicesMapping, DescribedPredicate<Dependency> predicate) {
            return new SliceDependencies(slice, classesToSlicesMapping, predicate);
        }
    }

    private static class EventRecorder {
        private static final String CYCLE_DETECTED_SECTION_INTRO = "Cycle detected: ";
        private static final String DEPENDENCY_DETAILS_INDENT = Strings.repeat(" ", 4);
        private static final Function<Edge<Slice, Dependency>, String> GET_FROM_NODE_DESCRIPTION = new Function<Edge<Slice, Dependency>, String>() {
            @Override
            public String apply(Edge<Slice, Dependency> input) {
                return input.getFrom().getDescription();
            }
        };

        private final CycleConfiguration cycleConfiguration = new CycleConfiguration();

        private EventRecorder() {
            log.debug("Maximum number of dependencies to report per edge is set to {}; "
                            + "this limit can be adapted using the `archunit.properties` value `{}=xxx`",
                    cycleConfiguration.getMaxNumberOfDependenciesToShowPerEdge(), MAX_NUMBER_OF_DEPENDENCIES_TO_SHOW_PER_EDGE_PROPERTY_NAME);
        }

        void record(Cycle<Slice, Dependency> cycle, ConditionEvents events) {
            events.add(newEvent(cycle));
        }

        private ConditionEvent newEvent(Cycle<Slice, Dependency> cycle) {
            Map<String, Edge<Slice, Dependency>> descriptionsToEdges = sortEdgesByDescription(cycle);
            String description = createDescription(descriptionsToEdges.keySet(), CYCLE_DETECTED_SECTION_INTRO.length());
            String details = createDetails(descriptionsToEdges);
            return new SimpleConditionEvent(cycle,
                    false,
                    CYCLE_DETECTED_SECTION_INTRO + description + lineSeparator() + details);
        }

        private Map<String, Edge<Slice, Dependency>> sortEdgesByDescription(Cycle<Slice, Dependency> cycle) {
            LinkedList<Edge<Slice, Dependency>> edges = new LinkedList<>(cycle.getEdges());
            Edge<Slice, Dependency> startEdge = Ordering.natural().onResultOf(GET_FROM_NODE_DESCRIPTION).min(edges);
            while (!edges.getFirst().equals(startEdge)) {
                edges.addLast(edges.pollFirst());
            }
            Map<String, Edge<Slice, Dependency>> descriptionToEdge = new LinkedHashMap<>();
            for (Edge<Slice, Dependency> edge : edges) {
                descriptionToEdge.put(edge.getFrom().getDescription(), edge);
            }
            return descriptionToEdge;
        }

        private String createDescription(Collection<String> edgeDescriptions, int indent) {
            List<String> descriptions = new ArrayList<>(edgeDescriptions);
            descriptions.add(descriptions.get(0));
            return Joiner.on(" -> " + lineSeparator() + Strings.repeat(" ", indent)).join(descriptions);
        }

        private String createDetails(Map<String, Edge<Slice, Dependency>> descriptionsToEdges) {
            List<String> details = new ArrayList<>();
            int sliceIndex = 0;
            for (Map.Entry<String, Edge<Slice, Dependency>> edgeWithDescription : descriptionsToEdges.entrySet()) {
                ++sliceIndex;
                details.add(String.format("  %d. Dependencies of %s", sliceIndex, edgeWithDescription.getKey()));
                details.addAll(dependenciesDescription(edgeWithDescription.getValue()));
            }
            return Joiner.on(lineSeparator()).join(details);
        }

        private List<String> dependenciesDescription(Edge<Slice, Dependency> edge) {
            List<String> result = new ArrayList<>();
            int maxDependencies = cycleConfiguration.getMaxNumberOfDependenciesToShowPerEdge();
            List<Dependency> allDependencies = edge.getAttachments();
            boolean tooManyDependenciesToDisplay = allDependencies.size() > maxDependencies;
            List<Dependency> dependenciesToDisplay = tooManyDependenciesToDisplay ? allDependencies.subList(0, maxDependencies) : allDependencies;
            for (Dependency dependency : dependenciesToDisplay) {
                result.add(DEPENDENCY_DETAILS_INDENT + "- " + dependency.getDescription());
            }
            if (tooManyDependenciesToDisplay) {
                result.add(DEPENDENCY_DETAILS_INDENT + String.format("(%d further dependencies have been omitted...)",
                        allDependencies.size() - dependenciesToDisplay.size()));
            }
            return result;
        }
    }
}
