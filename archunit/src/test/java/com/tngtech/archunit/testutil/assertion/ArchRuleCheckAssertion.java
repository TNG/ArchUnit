package com.tngtech.archunit.testutil.assertion;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.guava.api.Assertions.assertThat;

public class ArchRuleCheckAssertion {
    private final EvaluationResult evaluationResult;
    private final Optional<AssertionError> error;

    ArchRuleCheckAssertion(ArchRule rule, JavaClasses classes) {
        evaluationResult = rule.evaluate(classes);
        error = checkRule(rule, classes);
    }

    private Optional<AssertionError> checkRule(ArchRule rule, JavaClasses classes) {
        try {
            rule.check(classes);
            return Optional.absent();
        } catch (AssertionError error) {
            return Optional.of(error);
        }
    }

    public ArchRuleCheckAssertion hasViolationMatching(String regex) {
        for (String detail : evaluationResult.getFailureReport().getDetails()) {
            if (detail.matches(regex)) {
                return this;
            }
        }
        throw new AssertionError(String.format("No violation matches '%s'", regex));
    }

    public ArchRuleCheckAssertion hasNoViolationMatching(String regex) {
        for (String detail : evaluationResult.getFailureReport().getDetails()) {
            if (detail.matches(regex)) {
                throw new AssertionError(String.format("Violation matches '%s'", regex));
            }
        }
        return this;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public ArchRuleCheckAssertion hasOnlyOneViolationMatching(String regex) {
        assertThat(getOnlyElement(evaluationResult.getFailureReport().getDetails())).matches(regex);
        assertThat(error.get().getMessage()).containsPattern(regex);
        return this;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public ArchRuleCheckAssertion hasOnlyViolations(String... violations) {
        assertThat(evaluationResult.getFailureReport().getDetails()).containsOnly(violations);
        for (String violation : violations) {
            assertThat(error.get().getMessage()).contains(violation);
        }
        return this;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public ArchRuleCheckAssertion hasAnyViolationOf(String... violations) {
        assertThat(evaluationResult.getFailureReport().getDetails()).containsAnyOf(violations);
        assertThat(error.get().getMessage()).containsPattern(Joiner.on("|").join(violations));
        return this;
    }

    public ArchRuleCheckAssertion hasViolations(int numberOfViolations) {
        assertThat(evaluationResult.getFailureReport().getDetails()).as("number of violation").hasSize(numberOfViolations);
        return this;
    }

    public void hasNoViolation() {
        assertThat(evaluationResult.hasViolation()).as("result has violation").isFalse();
        assertThat(error).as("error").isAbsent();
    }
}
