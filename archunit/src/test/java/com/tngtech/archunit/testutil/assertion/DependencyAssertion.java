package com.tngtech.archunit.testutil.assertion;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.properties.HasName;
import org.assertj.core.api.AbstractBooleanAssert;
import org.assertj.core.api.AbstractObjectAssert;

import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependency;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependencyOrigin;
import static com.tngtech.archunit.core.domain.Dependency.Predicates.dependencyTarget;
import static java.util.regex.Pattern.quote;
import static org.assertj.core.api.Assertions.assertThat;

public class DependencyAssertion extends AbstractObjectAssert<DependencyAssertion, Dependency> {
    public DependencyAssertion(Dependency actual) {
        super(actual, DependencyAssertion.class);
    }

    public DependencyAssertion matches(Class<?> originClass, Class<?> targetClass) {
        for (AbstractBooleanAssert<?> dependencyAssert : dependencyMatches(originClass, targetClass)) {
            dependencyAssert.isTrue();
        }
        return this;
    }

    public DependencyAssertion doesntMatch(Class<?> originClass, Class<?> targetClass) {
        for (AbstractBooleanAssert<?> dependencyAssert : dependencyMatches(originClass, targetClass)) {
            dependencyAssert.isFalse();
        }
        return this;
    }

    private List<AbstractBooleanAssert<?>> dependencyMatches(Class<?> originClass, Class<?> targetClass) {
        return ImmutableList.of(
                assertThat(dependency(originClass, targetClass).test(actual))
                        .as("Dependency [%s -> %s] matches [%s -> %s]",
                                actual.getOriginClass().getName(), actual.getTargetClass().getName(),
                                originClass.getName(), targetClass.getName()),
                assertThat(dependency(originClass.getName(), targetClass.getName()).test(actual))
                        .as("Dependency [%s -> %s] matches [%s -> %s]",
                                actual.getOriginClass().getName(), actual.getTargetClass().getName(),
                                originClass.getName(), targetClass.getName()),
                assertThat(dependency(
                        HasName.Predicates.name(originClass.getName()),
                        HasName.Predicates.name(targetClass.getName())).test(actual))
                        .as("Dependency [%s -> %s] matches [%s -> %s]",
                                actual.getOriginClass().getName(), actual.getTargetClass().getName(),
                                originClass.getName(), targetClass.getName()));
    }

    public DependencyAssertion matchesOrigin(Class<?> originClass) {
        for (AbstractBooleanAssert<?> dependencyOriginAssert : dependencyMatchesOrigin(originClass)) {
            dependencyOriginAssert.isTrue();
        }
        return this;
    }

    public void doesntMatchOrigin(Class<?> originClass) {
        for (AbstractBooleanAssert<?> dependencyOriginAssert : dependencyMatchesOrigin(originClass)) {
            dependencyOriginAssert.isFalse();
        }
    }

    private List<AbstractBooleanAssert<?>> dependencyMatchesOrigin(Class<?> originClass) {
        return ImmutableList.of(
                assertThat(dependencyOrigin(originClass).test(actual))
                        .as("Dependency origin matches '%s.class'", originClass.getSimpleName()),
                assertThat(dependencyOrigin(originClass.getName()).test(actual))
                        .as("Dependency origin matches '%s.class'", originClass.getSimpleName()),
                assertThat(dependencyOrigin(HasName.Predicates.name(originClass.getName())).test(actual))
                        .as("Dependency origin matches '%s.class'", originClass.getSimpleName()));
    }

    public DependencyAssertion matchesTarget(Class<?> targetClass) {
        for (AbstractBooleanAssert<?> dependencyTargetAssert : dependencyMatchesTarget(targetClass)) {
            dependencyTargetAssert.isTrue();
        }
        return this;
    }

    public void doesntMatchTarget(Class<?> targetClass) {
        for (AbstractBooleanAssert<?> dependencyTargetAssert : dependencyMatchesTarget(targetClass)) {
            dependencyTargetAssert.isFalse();
        }
    }

    private List<AbstractBooleanAssert<?>> dependencyMatchesTarget(Class<?> targetClass) {
        return ImmutableList.of(
                assertThat(dependencyTarget(targetClass).test(actual))
                        .as("Dependency target matches '%s.class'", targetClass.getSimpleName()),
                assertThat(dependencyTarget(targetClass.getName()).test(actual))
                        .as("Dependency target matches '%s.class'", targetClass.getSimpleName()),
                assertThat(dependencyTarget(HasName.Predicates.name(targetClass.getName())).test(actual))
                        .as("Dependency target matches '%s.class'", targetClass.getSimpleName()));
    }

    public LocationAssertion hasDescription(String description) {
        assertThat(actual.getDescription()).isEqualTo(description);
        return new LocationAssertion();
    }

    public LocationAssertion hasDescription(String originDescription, String dependencyDescription, String targetDescription) {
        String expectedOriginDependsOnTargetDescription = quote(String.format("<%s> %s <%s>", originDescription, dependencyDescription, targetDescription));
        String descriptionPattern = String.format(".+ %s in \\([^ ]+:\\d+\\)", expectedOriginDependsOnTargetDescription);
        assertThat(actual.getDescription()).matches(descriptionPattern);
        return new LocationAssertion();
    }

    public class LocationAssertion {
        public DependencyAssertion inLocation(Class<?> expectedLocationSource, int expectedLineNumber) {
            assertThat(actual.getDescription()).endsWith(String.format("in (%s.java:%d)", expectedLocationSource.getSimpleName(), expectedLineNumber));
            return DependencyAssertion.this;
        }
    }
}
