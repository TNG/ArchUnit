package com.tngtech.archunit.testutil.assertion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.domain.Dependency;
import org.assertj.core.api.AbstractIterableAssert;

import static org.assertj.core.api.Assertions.assertThat;

public class DependenciesAssertion extends AbstractIterableAssert<
        DependenciesAssertion, Iterable<? extends Dependency>, Dependency, DependencyAssertion> {

    public DependenciesAssertion(Iterable<Dependency> dependencies) {
        super(dependencies, DependenciesAssertion.class);
    }

    @Override
    protected DependencyAssertion toAssert(Dependency value, String description) {
        return new DependencyAssertion(value).as(description);
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

    public DependenciesAssertion containOnly(final ExpectedDependencies expectedDependencies) {
        FluentIterable<? extends Dependency> rest = FluentIterable.from(actual);
        for (final List<Class<?>> expectedDependency : expectedDependencies) {
            rest = rest.filter(new Predicate<Dependency>() {
                @Override
                public boolean apply(Dependency input) {
                    return !matches(input, expectedDependency.get(0), expectedDependency.get(1));
                }
            });
        }
        assertThat(rest.toSet()).as("unexpected elements").isEmpty();
        return this;
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

    public static ExpectedDependenciesCreator from(Class<?> origin) {
        return new ExpectedDependenciesCreator(new ExpectedDependencies(), origin);
    }

    public static class ExpectedDependenciesCreator {
        private final ExpectedDependencies expectedDependencies;
        private final Class<?> origin;

        private ExpectedDependenciesCreator(ExpectedDependencies expectedDependencies, Class<?> origin) {
            this.expectedDependencies = expectedDependencies;
            this.origin = origin;
        }

        public ExpectedDependencies to(Class<?> target) {
            return expectedDependencies.add(origin, target);
        }
    }

    public static class ExpectedDependencies implements Iterable<List<Class<?>>> {
        List<List<Class<?>>> expectedDependencies = new ArrayList<>();

        private ExpectedDependencies() {
        }

        @Override
        public Iterator<List<Class<?>>> iterator() {
            return expectedDependencies.iterator();
        }

        public ExpectedDependenciesCreator from(Class<?> origin) {
            return new ExpectedDependenciesCreator(this, origin);
        }

        ExpectedDependencies add(Class<?> origin, Class<?> target) {
            expectedDependencies.add(ImmutableList.of(origin, target));
            return this;
        }
    }
}
