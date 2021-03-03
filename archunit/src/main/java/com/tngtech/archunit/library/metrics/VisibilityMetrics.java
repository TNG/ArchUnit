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
package com.tngtech.archunit.library.metrics;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;

import com.google.common.collect.ComparisonChain;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.library.metrics.components.MetricsComponent;
import com.tngtech.archunit.library.metrics.components.MetricsComponents;
import com.tngtech.archunit.library.metrics.rendering.AsciiDocTable;

import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.domain.JavaModifier.SYNTHETIC;
import static com.tngtech.archunit.library.metrics.MathUtils.divideSafely;

public class VisibilityMetrics {
    private static final DecimalFormat TWO_DIGITS = new DecimalFormat("0.00");

    private final SortedSet<VisibilityMeasurement> visibilityMeasurements;
    private final AverageRelativeVisibility averageRelativeVisibility;
    private final GlobalRelativeVisibility globalRelativeVisibility;

    private VisibilityMetrics(MetricsComponents<JavaClass> components) {
        ImmutableSortedSet.Builder<VisibilityMeasurement> visibilityMeasurementsBuilder = ImmutableSortedSet.naturalOrder();
        for (MetricsComponent<JavaClass> component : components) {
            visibilityMeasurementsBuilder.add(new VisibilityMeasurement(component));
        }
        visibilityMeasurements = visibilityMeasurementsBuilder.build();
        averageRelativeVisibility = new AverageRelativeVisibility(visibilityMeasurements);
        globalRelativeVisibility = new GlobalRelativeVisibility(visibilityMeasurements);
    }

    public AsciiDocTable toAsciiDocTable() {
        AsciiDocTable.Creator tableCreator = AsciiDocTable.intro()
                .addLine(String.format("Average Relative Visibility: Classes %s / Methods %s",
                        TWO_DIGITS.format(averageRelativeVisibility.averageClassVisibility), TWO_DIGITS.format(averageRelativeVisibility.averageMethodVisibility)))
                .addLine(String.format("Global Relative Visibility: Classes %s / Methods %s",
                        TWO_DIGITS.format(globalRelativeVisibility.globalClassVisibility), TWO_DIGITS.format(globalRelativeVisibility.globalMethodVisibility)))
                .header()
                .addColumnValue("Component Name")
                .addColumnValue("Relative Class Visibility")
                .addColumnValue("Relative Method Visibility")
                .end();

        for (VisibilityMeasurement measurement : visibilityMeasurements) {
            tableCreator.row()
                    .addColumnValue(measurement.getName())
                    .addColumnValue(TWO_DIGITS.format(measurement.getRelativeClassVisibility()))
                    .addColumnValue(TWO_DIGITS.format(measurement.getRelativeMethodVisibility()))
                    .end();
        }
        return tableCreator.create();
    }

    public static VisibilityMetrics of(MetricsComponents<JavaClass> components) {
        return new VisibilityMetrics(components);
    }

    private static class VisibilityMeasurement implements Comparable<VisibilityMeasurement> {
        private final String identifier;
        private final String name;
        private final Set<JavaClass> allClasses;
        private final Set<JavaClass> visibleClasses;
        private final Set<JavaMethod> allMethods;
        private final Set<JavaMethod> visibleMethods;

        private VisibilityMeasurement(MetricsComponent<JavaClass> component) {
            identifier = component.getIdentifier();
            name = component.getName();
            allClasses = getAllRelevantClasses(component);
            visibleClasses = getVisibleClasses(allClasses);
            allMethods = getAllMethods(allClasses);
            visibleMethods = getVisibleMethods(allClasses);
        }

        private Set<JavaClass> getAllRelevantClasses(MetricsComponent<JavaClass> component) {
            ImmutableSet.Builder<JavaClass> result = ImmutableSet.builder();
            for (JavaClass javaClass : component) {
                if (!javaClass.isAnonymousClass() && !javaClass.isLocalClass() && !javaClass.getModifiers().contains(SYNTHETIC) && !javaClass.isArray()) {
                    result.add(javaClass);
                }
            }
            return result.build();
        }

        private static Set<JavaClass> getVisibleClasses(Iterable<JavaClass> classes) {
            ImmutableSet.Builder<JavaClass> result = ImmutableSet.builder();
            for (JavaClass javaClass : classes) {
                if (javaClass.getModifiers().contains(PUBLIC)) {
                    result.add(javaClass);
                }
            }
            return result.build();
        }

        private static Set<JavaMethod> getAllMethods(Iterable<JavaClass> classes) {
            ImmutableSet.Builder<JavaMethod> result = ImmutableSet.builder();
            for (JavaClass javaClass : classes) {
                result.addAll(javaClass.getMethods());
            }
            return result.build();
        }

        private static Set<JavaMethod> getVisibleMethods(Iterable<JavaClass> classes) {
            ImmutableSet.Builder<JavaMethod> result = ImmutableSet.builder();
            for (JavaClass javaClass : classes) {
                for (JavaMethod method : javaClass.getMethods()) {
                    if (method.getModifiers().contains(PUBLIC) && declaringClassIsPublic(method)) {
                        result.add(method);
                    }
                }
            }
            return result.build();
        }

        private static boolean declaringClassIsPublic(JavaMethod method) {
            JavaClass enclosingClass = method.getOwner();
            while (enclosingClass != null) {
                if (!enclosingClass.getModifiers().contains(PUBLIC)) {
                    return false;
                }
                enclosingClass = enclosingClass.getEnclosingClass().orNull();
            }
            return true;
        }

        String getName() {
            return name;
        }

        Set<JavaClass> getAllClasses() {
            return allClasses;
        }

        Set<JavaClass> getVisibleClasses() {
            return visibleClasses;
        }

        Set<JavaMethod> getAllMethods() {
            return allMethods;
        }

        Set<JavaMethod> getVisibleMethods() {
            return visibleMethods;
        }

        double getRelativeClassVisibility() {
            return divideSafely(visibleClasses.size(), allClasses.size());
        }

        double getRelativeMethodVisibility() {
            return divideSafely(visibleMethods.size(), allMethods.size());
        }

        @Override
        public int compareTo(VisibilityMeasurement other) {
            return ComparisonChain.start()
                    .compare(getName(), other.getName())
                    .compare(identifier, other.identifier)
                    .result();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof VisibilityMeasurement)) {
                return false;
            }
            VisibilityMeasurement that = (VisibilityMeasurement) o;
            return Objects.equals(identifier, that.identifier);
        }

        @Override
        public int hashCode() {
            return Objects.hash(identifier);
        }
    }

    private static class AverageRelativeVisibility {
        final double averageClassVisibility;
        final double averageMethodVisibility;

        AverageRelativeVisibility(Collection<VisibilityMeasurement> measurements) {
            List<VisibilityMeasurement> measurementList = ImmutableList.copyOf(measurements);
            double averageClassVisibility = 0;
            double averageMethodVisibility = 0;
            for (int i = 1; i <= measurementList.size(); i++) {
                averageClassVisibility += (measurementList.get(i - 1).getRelativeClassVisibility() - averageClassVisibility) / i;
                averageMethodVisibility += (measurementList.get(i - 1).getRelativeMethodVisibility() - averageMethodVisibility) / i;
            }
            this.averageClassVisibility = averageClassVisibility;
            this.averageMethodVisibility = averageMethodVisibility;
        }
    }

    private static class GlobalRelativeVisibility {
        final double globalClassVisibility;
        final double globalMethodVisibility;

        GlobalRelativeVisibility(Collection<VisibilityMeasurement> measurements) {
            int totalNumberOfClasses = 0;
            int totalNumberOfVisibleClasses = 0;
            int totalNumberOfMethods = 0;
            int totalNumberOfVisibleMethods = 0;
            for (VisibilityMeasurement measurement : measurements) {
                totalNumberOfClasses += measurement.getAllClasses().size();
                totalNumberOfVisibleClasses += measurement.getVisibleClasses().size();
                totalNumberOfMethods += measurement.getAllMethods().size();
                totalNumberOfVisibleMethods += measurement.getVisibleMethods().size();
            }
            globalClassVisibility = divideSafely(totalNumberOfVisibleClasses, totalNumberOfClasses);
            globalMethodVisibility = divideSafely(totalNumberOfVisibleMethods, totalNumberOfMethods);
        }
    }
}
