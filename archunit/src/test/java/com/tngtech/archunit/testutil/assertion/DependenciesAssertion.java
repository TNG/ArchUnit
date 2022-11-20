package com.tngtech.archunit.testutil.assertion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import org.assertj.core.api.AbstractIterableAssert;

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.stream;
import static java.util.regex.Pattern.quote;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class DependenciesAssertion extends AbstractIterableAssert<
        DependenciesAssertion, Iterable<Dependency>, Dependency, DependencyAssertion> {

    public DependenciesAssertion(Iterable<Dependency> dependencies) {
        super(dependencies, DependenciesAssertion.class);
    }

    @Override
    protected DependencyAssertion toAssert(Dependency value, String description) {
        return new DependencyAssertion(value).as(description);
    }

    @Override
    protected DependenciesAssertion newAbstractIterableAssert(Iterable<? extends Dependency> iterable) {
        return new DependenciesAssertion(ImmutableSet.copyOf(iterable));
    }

    public DependenciesAssertion contain(Class<?> expectedOrigin, Class<?> expectedTarget) {
        if (!thisContains(expectedOrigin, expectedTarget)) {
            throw new AssertionError(String.format("%s is not contained in %s",
                    formatDependency(expectedOrigin, expectedTarget), actual));
        }
        return this;
    }

    private boolean thisContains(Class<?> expectedOrigin, Class<?> expectedTarget) {
        for (Dependency dependency : actual) {
            if (matches(dependency, expectedOrigin, expectedTarget)) {
                return true;
            }
        }
        return false;
    }

    private boolean matches(Dependency dependency, Class<?> expectedOrigin, Class<?> expectedTarget) {
        return dependency.getOriginClass().isEquivalentTo(expectedOrigin) && dependency.getTargetClass().isEquivalentTo(expectedTarget);
    }

    public DependenciesAssertion doesNotContain(Class<?> expectedOrigin, Class<?> expectedTarget) {
        if (thisContains(expectedOrigin, expectedTarget)) {
            throw new AssertionError(String.format("%s is contained in %s",
                    formatDependency(expectedOrigin, expectedTarget), actual));
        }
        return this;
    }

    public DependenciesAssertion contain(ExpectedDependencies expectedDependencies) {
        matchExpectedDependencies(expectedDependencies)
                .assertNoMissingDependencies();
        return this;
    }

    public DependenciesAssertion containOnly(ExpectedDependencies expectedDependencies) {
        ExpectedDependenciesMatchResult result = matchExpectedDependencies(expectedDependencies);
        result.assertNoMissingDependencies();
        result.assertAllDependenciesMatched();
        return this;
    }

    private ExpectedDependenciesMatchResult matchExpectedDependencies(ExpectedDependencies expectedDependencies) {
        List<Dependency> rest = newArrayList(actual);
        List<ExpectedDependency> missingDependencies = new ArrayList<>();
        for (ExpectedDependency expectedDependency : expectedDependencies) {
            if (rest.stream().noneMatch(expectedDependency::matches)) {
                missingDependencies.add(expectedDependency);
            }
            rest = rest.stream().filter(dependency -> !expectedDependency.matches(dependency)).collect(toList());
        }
        return new ExpectedDependenciesMatchResult(missingDependencies, rest);
    }

    public DependenciesAssertion containOnly(Class<?> expectedOrigin, Class<?> expectedTarget) {
        for (Dependency dependency : actual) {
            toAssert(dependency, dependency.getDescription()).matches(expectedOrigin, expectedTarget);
        }
        return this;
    }

    private Object formatDependency(Class<?> origin, Class<?> target) {
        return String.format("Dependency [%s -> %s]", origin.getName(), target.getName());
    }

    public static ExpectedDependenciesCreator from(JavaClass origin) {
        return from(origin.reflect());
    }

    public static ExpectedDependenciesCreator from(Class<?> origin) {
        return new ExpectedDependenciesCreator(new ExpectedDependencies(), origin);
    }

    public static class ExpectedDependenciesCreator {
        private final ExpectedDependencies expectedDependencies;
        private Optional<String> descriptionTemplate = Optional.empty();
        private final Class<?> origin;

        private ExpectedDependenciesCreator(ExpectedDependencies expectedDependencies, Class<?> origin) {
            this.expectedDependencies = expectedDependencies;
            this.origin = origin;
        }

        public ExpectedDependenciesCreator withExpectedDescriptionTemplate(String descriptionTemplate) {
            this.descriptionTemplate = Optional.of(".*" + quote(descriptionTemplate) + ".*");
            return this;
        }

        public ExpectedDependenciesCreator withExpectedDescriptionPatternTemplate(String descriptionPatternTemplate) {
            this.descriptionTemplate = Optional.of(descriptionPatternTemplate);
            return this;
        }

        public ExpectedDependencies to(JavaClass... targets) {
            Class<?>[] reflectedTargets = stream(targets).map(JavaClass::reflect).toArray(Class<?>[]::new);
            return to(reflectedTargets);
        }

        public ExpectedDependencies to(Class<?>... targets) {
            for (Class<?> target : targets) {
                expectedDependencies.add(origin, target, descriptionTemplate);
            }
            return expectedDependencies;
        }
    }

    public static class ExpectedDependencies implements Iterable<ExpectedDependency> {
        List<ExpectedDependency> expectedDependencies = new ArrayList<>();

        private ExpectedDependencies() {
        }

        @Override
        public Iterator<ExpectedDependency> iterator() {
            return expectedDependencies.iterator();
        }

        public ExpectedDependenciesCreator from(JavaClass origin) {
            return from(origin.reflect());
        }

        public ExpectedDependenciesCreator from(Class<?> origin) {
            return new ExpectedDependenciesCreator(this, origin);
        }

        void add(Class<?> origin, Class<?> target, Optional<String> descriptionTemplate) {
            ExpectedDependency expectedDependency = new ExpectedDependency(origin, target);
            if (descriptionTemplate.isPresent()) {
                expectedDependency.descriptionMatching(descriptionTemplate.get().replace("#target", quote(target.getName())));
            }
            expectedDependencies.add(expectedDependency);
        }

        public ExpectedDependencies withDescriptionContaining(String descriptionTemplate, Object... args) {
            getLast(expectedDependencies).descriptionContaining(descriptionTemplate, args);
            return this;
        }

        public ExpectedDependencies withDescriptionMatching(String regexTemplate, Object... args) {
            Object[] quotedArgs = stream(args).map(arg -> quote(String.valueOf(arg))).toArray();
            String regex = String.format(regexTemplate, quotedArgs);
            getLast(expectedDependencies).descriptionMatching(regex);
            return this;
        }

        public ExpectedDependencies inLocation(Class<?> location, int lineNumber) {
            getLast(expectedDependencies).location(location, lineNumber);
            return this;
        }
    }

    private static class ExpectedDependency {
        private final Class<?> origin;
        private final Class<?> target;
        private Optional<Pattern> descriptionPattern = Optional.empty();
        private Optional<String> locationPart = Optional.empty();

        ExpectedDependency(Class<?> origin, Class<?> target) {
            this.origin = origin;
            this.target = target;
        }

        boolean matches(Dependency dependency) {
            return dependency.getOriginClass().isEquivalentTo(origin)
                    && dependency.getTargetClass().isEquivalentTo(target)
                    && (!descriptionPattern.isPresent() || descriptionPattern.get().matcher(dependency.getDescription()).matches())
                    && (!locationPart.isPresent() || dependency.getDescription().endsWith(locationPart.get()));
        }

        public void descriptionContaining(String descriptionTemplate, Object... args) {
            String descriptionPart = String.format(descriptionTemplate, args);
            descriptionPattern = Optional.of(Pattern.compile(".*" + quote(descriptionPart) + ".*"));
        }

        public void descriptionMatching(String regex) {
            descriptionPattern = Optional.of(Pattern.compile(regex));
        }

        public void location(Class<?> location, int lineNumber) {
            locationPart = Optional.of(String.format("in (%s.java:%d)", location.getSimpleName(), lineNumber));
        }

        @Override
        public String toString() {
            String dependency = origin.getName() + " -> " + target.getName();
            String location = locationPart.isPresent() ? " " + locationPart.get() : "";
            String description = descriptionPattern.isPresent() ? " with description matching '" + descriptionPattern.get() + "'" : "";
            return dependency + location + description;
        }
    }

    private class ExpectedDependenciesMatchResult {
        private final Iterable<ExpectedDependency> missingDependencies;
        private final Iterable<Dependency> unexpectedDependencies;

        private ExpectedDependenciesMatchResult(Iterable<ExpectedDependency> missingDependencies, Iterable<Dependency> unexpectedDependencies) {
            this.missingDependencies = missingDependencies;
            this.unexpectedDependencies = unexpectedDependencies;
        }

        void assertNoMissingDependencies() {
            if (!Iterables.isEmpty(missingDependencies)) {
                throw new AssertionError("Could not find expected dependencies:" + lineSeparator() + Joiner.on(lineSeparator()).join(missingDependencies)
                        + lineSeparator() + "within: " + lineSeparator() + Joiner.on(lineSeparator()).join(descriptionsOf(actual)));
            }
        }

        private List<String> descriptionsOf(Iterable<? extends HasDescription> haveDescriptions) {
            List<String> result = new ArrayList<>();
            for (HasDescription hasDescription : haveDescriptions) {
                result.add(hasDescription.getDescription());
            }
            return result;
        }

        public void assertAllDependenciesMatched() {
            assertThat(unexpectedDependencies).as("unexpected dependencies").isEmpty();
        }
    }
}
