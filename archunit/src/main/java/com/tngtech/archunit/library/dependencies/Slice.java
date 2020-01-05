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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.ForwardingSet;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.CanOverrideDescription;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * A collection of {@link JavaClass JavaClasses} modelling some domain aspect of a code basis. This is conceptually
 * a cut through a code base according to business logic. Take for example
 * <pre><code>
 * com.mycompany.myapp.order
 * com.mycompany.myapp.customer
 * com.mycompany.myapp.user
 * com.mycompany.myapp.authorization
 * </code></pre>
 * The top level packages under 'myapp' could be considered slices according to different domain aspects.<br>
 * Thus there could be a slice 'Order' housing all the classes from the {@code order} package, a slice 'Customer'
 * housing all the classes from the {@code customer} package and so on.
 */
public final class Slice extends ForwardingSet<JavaClass> implements HasDescription, CanOverrideDescription<Slice> {
    private final SliceAssignment sliceAssignment;
    private final List<String> matchingGroups;
    private final Description description;
    private final Set<JavaClass> classes;

    private Slice(SliceAssignment sliceAssignment, List<String> matchingGroups, Set<JavaClass> classes) {
        this(sliceAssignment,
                matchingGroups,
                new Description("Slice " + Joiner.on(" - ").join(ascendingCaptures(matchingGroups))),
                classes);
    }

    private Slice(SliceAssignment sliceAssignment, List<String> matchingGroups, Description description,
            Set<JavaClass> classes) {
        this.sliceAssignment = sliceAssignment;
        this.matchingGroups = checkNotNull(matchingGroups);
        this.description = checkNotNull(description);
        this.classes = ImmutableSet.copyOf(classes);
    }

    private static List<String> ascendingCaptures(List<String> matchingGroups) {
        List<String> result = new ArrayList<>();
        for (int i = 1; i <= matchingGroups.size(); i++) {
            result.add("$" + i);
        }
        return result;
    }

    @Override
    protected Set<JavaClass> delegate() {
        return classes;
    }

    @Override
    @PublicAPI(usage = ACCESS)
    public String getDescription() {
        return description.format(matchingGroups);
    }

    /**
     * The pattern can be a description with references to the matching groups by '$' and position.
     * E.g. slices are created by 'some.svc.(*).sub.(*)', and the pattern is "the module $2 of service $1",
     * and we match 'some.svc.foo.module.bar', then the resulting description will be
     * "the module bar of service foo".
     *
     * @param pattern The description pattern with numbered references of the form $i
     * @return Same slice with different description
     */
    @Override
    @PublicAPI(usage = ACCESS)
    public Slice as(String pattern) {
        return new Slice(sliceAssignment, matchingGroups, new Description(pattern), classes);
    }

    /**
     * @deprecated Use {@link #getDependenciesFromSelf()}
     */
    @Deprecated
    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getDependencies() {
        return getDependenciesFromSelf();
    }

    /**
     * @return All dependencies that originate from a class within this {@link Slice} and target a class outside of this {@link Slice}
     */
    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getDependenciesFromSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaClass javaClass : this) {
            for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                if (isNotAssignedToOwnSlice(dependency.getTargetClass())) {
                    result.add(dependency);
                }
            }
        }
        return result.build();
    }

    /**
     * @return All dependencies that originate from outside of this {@link Slice} and target a class within this {@link Slice}
     */
    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getDependenciesToSelf() {
        ImmutableSet.Builder<Dependency> result = ImmutableSet.builder();
        for (JavaClass javaClass : this) {
            for (Dependency dependency : javaClass.getDirectDependenciesToSelf()) {
                if (isNotAssignedToOwnSlice(dependency.getOriginClass())) {
                    result.add(dependency);
                }
            }
        }
        return result.build();
    }

    // We cannot check 'contains' here, because there might be classes in the same slice that have not been originally imported (e.g. array types)
    private boolean isNotAssignedToOwnSlice(JavaClass javaClass) {
        List<String> dependencyIdentifier = sliceAssignment.getIdentifierOf(javaClass).getParts();
        return !dependencyIdentifier.equals(matchingGroups);
    }

    @Override
    public String toString() {
        return getDescription();
    }

    /**
     * Returns a matching part of this slice. E.g. if the slice was created by matching '..(*).controller.(*)..',
     * against 'some.other.controller.here.more', then name part '1' would be 'other' and name part '2' would
     * be 'here'.
     *
     * @param index The index of the matched group
     * @return The part of the matched package name.
     */
    @PublicAPI(usage = ACCESS)
    public String getNamePart(int index) {
        checkArgument(index > 0 && index <= matchingGroups.size(), "Found no name part with index %d", index);
        return matchingGroups.get(index - 1);
    }

    private static class Description {
        private final String pattern;

        private Description(String pattern) {
            this.pattern = pattern;
        }

        String format(List<String> matchingGroups) {
            String result = pattern;
            for (int i = 1; i <= matchingGroups.size(); i++) {
                result = result.replace("$" + i, matchingGroups.get(i - 1));
            }
            return result;
        }
    }

    static class Builder {
        private final List<String> matchingGroups;
        private final SliceAssignment sliceAssignment;
        private final Set<JavaClass> classes = new HashSet<>();

        private Builder(List<String> matchingGroups, SliceAssignment sliceAssignment) {
            this.matchingGroups = matchingGroups;
            this.sliceAssignment = sliceAssignment;
        }

        static Builder from(List<String> matchingGroups, SliceAssignment sliceAssignment) {
            return new Builder(matchingGroups, sliceAssignment);
        }

        Builder addClass(JavaClass clazz) {
            classes.add(clazz);
            return this;
        }

        Slice build() {
            return new Slice(sliceAssignment, matchingGroups, classes);
        }
    }
}
