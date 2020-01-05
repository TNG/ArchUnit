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
package com.tngtech.archunit.library.dependencies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.ForwardingSet;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Ordering;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.base.Guava;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

class SliceCycleArchCondition extends ArchCondition<Slice> {
    private final DescribedPredicate<Dependency> predicate;
    private ClassesToSlicesMapping classesToSlicesMapping;
    private DependencyGraph graph;
    private EventRecorder eventRecorder;

    SliceCycleArchCondition(DescribedPredicate<Dependency> predicate) {
        super("be free of cycles");
        this.predicate = predicate;
    }

    @Override
    public void init(Iterable<Slice> allSlices) {
        initializeResources(allSlices);
        for (Slice slice : allSlices) {
            graph.add(slice, Collections.<Edge<Slice, Dependency>>emptySet());
        }
    }

    private void initializeResources(Iterable<Slice> allSlices) {
        classesToSlicesMapping = new ClassesToSlicesMapping(allSlices);
        graph = new DependencyGraph();
        eventRecorder = new EventRecorder();
    }

    @Override
    public void check(Slice slice, ConditionEvents events) {
        graph.add(slice, SliceDependencies.of(slice, classesToSlicesMapping, predicate));
    }

    @Override
    public void finish(ConditionEvents events) {
        for (Cycle<Slice, Dependency> cycle : graph.getCycles()) {
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

    private static class DependencyGraph extends Graph<Slice, Dependency> {

    }

    private static class SliceDependencies extends ForwardingSet<Edge<Slice, Dependency>> {
        private final Set<Edge<Slice, Dependency>> edges;

        private SliceDependencies(Slice slice, ClassesToSlicesMapping classesToSlicesMapping, DescribedPredicate<Dependency> predicate) {
            Multimap<Slice, Dependency> targetSlicesWithDependencies = targetsOf(slice, classesToSlicesMapping, predicate);
            ImmutableSet.Builder<Edge<Slice, Dependency>> edgeBuilder = ImmutableSet.builder();
            for (Map.Entry<Slice, Collection<Dependency>> entry : targetSlicesWithDependencies.asMap().entrySet()) {
                edgeBuilder.add(new Edge<>(slice, entry.getKey(), entry.getValue()));
            }
            this.edges = edgeBuilder.build();
        }

        private Multimap<Slice, Dependency> targetsOf(Slice slice,
                ClassesToSlicesMapping classesToSlicesMapping, DescribedPredicate<Dependency> predicate) {
            Multimap<Slice, Dependency> result = HashMultimap.create();
            for (Dependency dependency : Guava.Iterables.filter(slice.getDependenciesFromSelf(), predicate)) {
                if (classesToSlicesMapping.containsKey(dependency.getTargetClass())) {
                    result.put(classesToSlicesMapping.get(dependency.getTargetClass()), dependency);
                }
            }
            return result;
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
        private static final String MESSAGE_TEMPLATE = "Cycle detected: %s%n%s";
        private static final Function<Edge<Slice, Dependency>, String> GET_FROM_NODE_DESCRIPTION = new Function<Edge<Slice, Dependency>, String>() {
            @Override
            public String apply(Edge<Slice, Dependency> input) {
                return input.getFrom().getDescription();
            }
        };

        void record(Cycle<Slice, Dependency> cycle, ConditionEvents events) {
            events.add(newEvent(cycle));
        }

        private ConditionEvent newEvent(Cycle<Slice, Dependency> cycle) {
            Map<String, Edge<Slice, Dependency>> descriptionsToEdges = sortEdgesByDescription(cycle);
            String description = createDescription(descriptionsToEdges);
            String details = createDetails(descriptionsToEdges);
            return new SimpleConditionEvent(cycle,
                    false,
                    String.format(MESSAGE_TEMPLATE, description, details));
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

        private String createDescription(Map<String, Edge<Slice, Dependency>> descriptionsToEdges) {
            List<String> descriptions = new ArrayList<>(descriptionsToEdges.keySet());
            descriptions.add(descriptions.get(0));
            return Joiner.on(" -> ").join(descriptions);
        }

        private String createDetails(Map<String, Edge<Slice, Dependency>> descriptionsToEdges) {
            List<String> details = new ArrayList<>();
            for (Map.Entry<String, Edge<Slice, Dependency>> edgeWithDescription : descriptionsToEdges.entrySet()) {
                details.add(String.format("Dependencies of %s", edgeWithDescription.getKey()));
                details.addAll(dependenciesDescription(edgeWithDescription));
            }
            return Joiner.on(System.lineSeparator()).join(details);
        }

        private List<String> dependenciesDescription(Map.Entry<String, Edge<Slice, Dependency>> edgeWithDescription) {
            List<String> result = new ArrayList<>();
            for (Dependency dependency : new TreeSet<>(edgeWithDescription.getValue().getAttachments())) {
                result.add(dependency.getDescription());
            }
            return result;
        }
    }
}
