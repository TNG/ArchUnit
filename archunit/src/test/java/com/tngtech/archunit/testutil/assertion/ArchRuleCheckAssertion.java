package com.tngtech.archunit.testutil.assertion;

import java.util.List;
import java.util.Optional;

import com.google.common.base.Joiner;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.assertj.core.api.Assertions.assertThat;

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
            return Optional.empty();
        } catch (AssertionError error) {
            return Optional.of(error);
        }
    }

    public ArchRuleCheckAssertion hasViolationContaining(String part, Object... args) {
        String expectedPart = String.format(part, args);
        assertThat(evaluationResult.getFailureReport().getDetails())
                .as("violation details (should have some detail containing '%s')", expectedPart)
                .anyMatch(detail -> detail.contains(expectedPart));
        return this;
    }

    public ArchRuleCheckAssertion hasViolationMatching(String regex) {
        assertThat(evaluationResult.getFailureReport().getDetails())
                .as("violation details (should have some detail matching '%s')", regex)
                .anyMatch(detail -> detail.matches(regex));
        return this;
    }

    public ArchRuleCheckAssertion hasNoViolationContaining(String part, Object... args) {
        String expectedPart = String.format(part, args);
        assertThat(evaluationResult.getFailureReport().getDetails())
                .as("violation details (should not have any detail containing '%s')", expectedPart)
                .noneMatch(detail -> detail.contains(expectedPart));
        return this;
    }

    public ArchRuleCheckAssertion hasNoViolationMatching(String regex) {
        assertThat(evaluationResult.getFailureReport().getDetails())
                .as("violation details (should not have any detail matching '%s')", regex)
                .noneMatch(detail -> detail.matches(regex));
        return this;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public ArchRuleCheckAssertion hasOnlyOneViolation(String violationMessage) {
        assertThat(evaluationResult.getFailureReport().getDetails()).as("Failure report details")
                .hasSize(1)
                .first().isEqualTo(violationMessage);
        assertThat(error.get().getMessage()).contains(violationMessage);
        return this;
    }

    public ArchRuleCheckAssertion hasOnlyOneViolationWithStandardPattern(Class<?> violatingClass, String violationDescription) {
        String violationMessage = toViolationMessage(violatingClass, violationDescription);
        return hasOnlyOneViolation(violationMessage);
    }

    public ArchRuleCheckAssertion hasViolationWithStandardPattern(Class<?> violatingClass, String violationDescription) {
        String violationMessage = toViolationMessage(violatingClass, violationDescription);
        List<String> allViolations = evaluationResult.getFailureReport().getDetails();
        assertThat(allViolations).contains(violationMessage);
        assertThat(error.get().getMessage()).contains(violationMessage);
        return this;
    }

    private String toViolationMessage(Class<?> violatingClass, String violationDescription) {
        return "Class <" + violatingClass.getName() + "> " + violationDescription + " in (" + violatingClass.getSimpleName() + ".java:0)";
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    public ArchRuleCheckAssertion hasOnlyOneViolationContaining(String part) {
        assertThat(getOnlyElement(evaluationResult.getFailureReport().getDetails())).contains(part);
        assertThat(error.get().getMessage()).contains(part);
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

    public ArchRuleCheckAssertion hasNumberOfViolations(int numberOfViolations) {
        assertThat(evaluationResult.getFailureReport().getDetails()).as("number of violation").hasSize(numberOfViolations);
        return this;
    }

    public void hasNoViolation() {
        assertThat(evaluationResult.hasViolation()).as("result has violation").isFalse();
        assertThat(error).as("error").isEmpty();
    }
}
