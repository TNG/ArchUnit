/*
 * Copyright 2017 TNG Technology Consulting GmbH
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
import com.google.common.collect.ForwardingSet;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.properties.CanOverrideDescription;

import static com.google.common.base.Preconditions.checkArgument;
import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

public final class Slice extends ForwardingSet<JavaClass> implements HasDescription, CanOverrideDescription<Slice> {
    private final List<String> matchingGroups;
    private Description description;
    private final Set<JavaClass> classes;

    private Slice(List<String> matchingGroups, Set<JavaClass> classes) {
        this.matchingGroups = matchingGroups;
        this.description = new Description("Slice " + Joiner.on(" - ").join(ascendingCaptures(matchingGroups)));
        this.classes = ImmutableSet.copyOf(classes);
    }

    private List<String> ascendingCaptures(List<String> matchingGroups) {
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
    public Slice as(String pattern) {
        description = new Description(pattern);
        return this;
    }

    @PublicAPI(usage = ACCESS)
    public Set<Dependency> getDependencies() {
        Set<Dependency> result = new HashSet<>();
        for (JavaClass javaClass : this) {
            for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
                if (!contains(dependency.getTargetClass())) {
                    result.add(dependency);
                }
            }
        }
        return result;
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
        private final Set<JavaClass> classes = new HashSet<>();

        private Builder(List<String> matchingGroups) {
            this.matchingGroups = matchingGroups;
        }

        static Builder from(List<String> matchingGroups) {
            return new Builder(matchingGroups);
        }

        Builder addClass(JavaClass clazz) {
            classes.add(clazz);
            return this;
        }

        Slice build() {
            return new Slice(matchingGroups, classes);
        }
    }
}
