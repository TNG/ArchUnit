package com.tngtech.archunit.library.slices;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ForwardingSet;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.Dependency;
import com.tngtech.archunit.core.HasDescription;
import com.tngtech.archunit.core.JavaClass;

public class Slice extends ForwardingSet<JavaClass> implements HasDescription {
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
    public Slice as(String pattern) {
        description = new Description(pattern);
        return this;
    }

    public Set<Dependency> getDependencies() {
        Set<Dependency> result = new HashSet<>();
        for (JavaClass javaClass : this) {
            for (Dependency dependency : javaClass.getDirectDependencies()) {
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

    public static class Builder {
        private final List<String> matchingGroups;
        private Set<JavaClass> classes = new HashSet<>();

        private Builder(List<String> matchingGroups) {
            this.matchingGroups = matchingGroups;
        }

        static Builder from(List<String> matchingGroups) {
            return new Builder(matchingGroups);
        }

        public Builder addClass(JavaClass clazz) {
            classes.add(clazz);
            return this;
        }

        public Slice build() {
            return new Slice(matchingGroups, classes);
        }
    }
}
