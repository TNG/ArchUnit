package com.tngtech.archunit.library.dependencies;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
import com.tngtech.archunit.core.Dependency;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;

class SlicesArchCondition extends ArchCondition<Slice> {
    private final ClassesToSlicesMapping classesToSlicesMapping = new ClassesToSlicesMapping();
    private DependencyGraph graph;
    private final EventRecorder eventRecorder = new EventRecorder();

    @Override
    public void check(Slice slice, ConditionEvents events) {
        initGraph();
        graph.add(slice, SliceDependencies.of(slice, classesToSlicesMapping));
        for (Cycle<Slice, Dependency> cycle : graph.getCycles()) {
            eventRecorder.record(cycle, events);
        }
    }

    private void initGraph() {
        if (graph != null) {
            return;
        }

        graph = new DependencyGraph();
        for (Slice slice : allObjectsToTest()) {
            graph.add(slice, Collections.<Edge<Slice, Dependency>>emptySet());
        }
    }

    private class ClassesToSlicesMapping {
        private Map<JavaClass, Slice> mapping;

        public Slice get(JavaClass javaClass) {
            return mapping().get(javaClass);
        }

        private Map<JavaClass, Slice> mapping() {
            if (mapping != null) {
                return mapping;
            }
            ImmutableMap.Builder<JavaClass, Slice> result = ImmutableMap.builder();
            for (Slice slice : allObjectsToTest()) {
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

        private SliceDependencies(Slice slice, ClassesToSlicesMapping classesToSlicesMapping) {
            Multimap<Slice, Dependency> targetSlicesWithDependencies = targetsOf(slice, classesToSlicesMapping);
            ImmutableSet.Builder<Edge<Slice, Dependency>> edgeBuilder = ImmutableSet.builder();
            for (Map.Entry<Slice, Collection<Dependency>> entry : targetSlicesWithDependencies.asMap().entrySet()) {
                edgeBuilder.add(new Edge<>(slice, entry.getKey(), entry.getValue()));
            }
            this.edges = edgeBuilder.build();
        }

        private Multimap<Slice, Dependency> targetsOf(Slice slice, ClassesToSlicesMapping classesToSlicesMapping) {
            Multimap<Slice, Dependency> result = HashMultimap.create();
            for (Dependency dependency : slice.getDependencies()) {
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

        static SliceDependencies of(Slice slice, ClassesToSlicesMapping classesToSlicesMapping) {
            return new SliceDependencies(slice, classesToSlicesMapping);
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

        private final Set<Cycle<Slice, Dependency>> alreadyRecorded = new HashSet<>();

        public void record(Cycle<Slice, Dependency> cycle, ConditionEvents events) {
            if (alreadyRecorded.contains(cycle)) {
                return;
            }
            events.add(newEvent(cycle));
            alreadyRecorded.add(cycle);
        }

        private ConditionEvent newEvent(Cycle<Slice, Dependency> cycle) {
            Map<String, Edge<Slice, Dependency>> descriptionsToEdges = sortEdgesByDescription(cycle);
            String description = createDescription(descriptionsToEdges);
            String details = createDetails(descriptionsToEdges);
            return new ConditionEvent(false, MESSAGE_TEMPLATE, description, details);
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
