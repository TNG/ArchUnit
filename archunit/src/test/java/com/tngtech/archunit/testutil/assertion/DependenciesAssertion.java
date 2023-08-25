package com.tngtech.archunit.testutil.assertion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import org.assertj.core.api.AbstractCollectionAssert;

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.collect.Lists.newArrayList;
import static java.lang.System.lineSeparator;
import static java.util.Arrays.stream;
import static java.util.regex.Pattern.quote;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

public class DependenciesAssertion extends AbstractCollectionAssert<
        DependenciesAssertion, Collection<Dependency>, Dependency, DependencyAssertion> {

    public DependenciesAssertion(Collection<Dependency> dependencies) {
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
        ExpectedDependencies.MatchResult result = matchExpectedDependencies(expectedDependencies);
        result.assertNoMissingDependencies();
        result.assertAllDependenciesMatched();
        return this;
    }

    private ExpectedDependencies.MatchResult matchExpectedDependencies(ExpectedDependencies expectedDependencies) {
        return expectedDependencies.match(actual);
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
            descriptionTemplate.ifPresent(s -> expectedDependency.descriptionMatching(s.replace("#target", quote(target.getName()))));
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

        public MatchResult match(Collection<Dependency> actualDependencies) {
            List<Dependency> rest = newArrayList(actualDependencies);
            List<ExpectedDependency> missingDependencies = new ArrayList<>();
            for (ExpectedDependency expectedDependency : expectedDependencies) {
                if (rest.stream().noneMatch(expectedDependency::matches)) {
                    missingDependencies.add(expectedDependency);
                }
                rest = rest.stream().filter(dependency -> !expectedDependency.matches(dependency)).collect(toList());
            }
            return new MatchResult(actualDependencies, missingDependencies, rest);
        }

        public static class MatchResult {
            private final Collection<Dependency> actualDependencies;
            private final Collection<ExpectedDependency> missingDependencies;
            private final Collection<Dependency> unexpectedDependencies;

            private MatchResult(Collection<Dependency> actualDependencies, Collection<ExpectedDependency> missingDependencies, Collection<Dependency> unexpectedDependencies) {
                this.actualDependencies = actualDependencies;
                this.missingDependencies = missingDependencies;
                this.unexpectedDependencies = unexpectedDependencies;
            }

            public void assertNoMissingDependencies() {
                if (!Iterables.isEmpty(missingDependencies)) {
                    throw new AssertionError("Could not find expected dependencies:" + lineSeparator()
                            + missingDependencies.stream().map(Objects::toString).collect(joining(lineSeparator())) + lineSeparator()
                            + "within: " + lineSeparator()
                            + descriptionsOf(actualDependencies).collect(joining(lineSeparator())));
                }
            }

            private Stream<String> descriptionsOf(Collection<? extends HasDescription> haveDescriptions) {
                return haveDescriptions.stream().map(HasDescription::getDescription);
            }

            public void assertAllDependenciesMatched() {
                assertThat(unexpectedDependencies).as("unexpected dependencies").isEmpty();
            }

            public boolean matchesExactly() {
                return unexpectedDependencies.isEmpty() && missingDependencies.isEmpty();
            }
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
            String location = locationPart.map(s -> " " + s).orElse("");
            String description = descriptionPattern.map(pattern -> " with description matching '" + pattern + "'").orElse("");
            return dependency + location + description;
        }
    }
}
