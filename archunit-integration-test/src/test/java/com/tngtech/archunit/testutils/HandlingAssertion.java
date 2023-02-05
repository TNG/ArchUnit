package com.tngtech.archunit.testutils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.lang.EvaluationResult;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Sets.union;
import static java.lang.System.lineSeparator;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toSet;

class HandlingAssertion {
    private final Set<ExpectedRelation> expectedFieldAccesses;
    private final Set<ExpectedRelation> expectedMethodCalls;
    private final Set<ExpectedRelation> expectedConstructorCalls;
    private final Set<ExpectedRelation> expectedDependencies;

    private HandlingAssertion() {
        this(new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    private HandlingAssertion(
            Set<ExpectedRelation> expectedFieldAccesses,
            Set<ExpectedRelation> expectedMethodCalls,
            Set<ExpectedRelation> expectedConstructorCalls,
            Set<ExpectedRelation> expectedDependencies) {

        this.expectedFieldAccesses = expectedFieldAccesses;
        this.expectedMethodCalls = expectedMethodCalls;
        this.expectedConstructorCalls = expectedConstructorCalls;
        this.expectedDependencies = expectedDependencies;
    }

    void byFieldAccess(ExpectedRelation access) {
        expectedFieldAccesses.add(access);
    }

    void byConstructorCall(ExpectedRelation call) {
        expectedConstructorCalls.add(call);
    }

    void byMethodCall(ExpectedRelation call) {
        expectedMethodCalls.add(call);
    }

    void byDependency(ExpectedRelation inheritance) {
        expectedDependencies.add(inheritance);
    }

    static HandlingAssertion ofRule() {
        return new HandlingAssertion();
    }

    Result evaluate(EvaluationResult evaluationResult) {
        Result result = new Result();
        result.addAll(evaluateFieldAccesses(evaluationResult));
        result.addAll(evaluateMethodCalls(evaluationResult));
        result.addAll(evaluateConstructorCalls(evaluationResult));
        result.addAll(evaluateCalls(evaluationResult));
        result.addAll(evaluateAccesses(evaluationResult));
        result.addAll(evaluateDependencies(evaluationResult));
        return result;
    }

    private Set<String> evaluateFieldAccesses(EvaluationResult result) {
        Set<String> errorMessages = new HashSet<>();
        Set<ExpectedRelation> left = new HashSet<>(this.expectedFieldAccesses);
        result.handleViolations((Collection<JavaFieldAccess> violatingObjects, String message) ->
                errorMessages.addAll(removeExpectedAccesses(violatingObjects, left)));
        return union(errorMessages, errorMessagesFrom(left));
    }

    private Set<String> evaluateMethodCalls(EvaluationResult result) {
        Set<String> errorMessages = new HashSet<>();
        Set<ExpectedRelation> left = new HashSet<>(expectedMethodCalls);
        result.handleViolations((Collection<JavaMethodCall> violatingObjects, String message) ->
                errorMessages.addAll(removeExpectedAccesses(violatingObjects, left)));
        return union(errorMessages, errorMessagesFrom(left));
    }

    private Set<String> evaluateConstructorCalls(EvaluationResult result) {
        Set<String> errorMessages = new HashSet<>();
        Set<ExpectedRelation> left = new HashSet<>(expectedConstructorCalls);
        result.handleViolations((Collection<JavaConstructorCall> violatingObjects, String message) ->
                errorMessages.addAll(removeExpectedAccesses(violatingObjects, left)));
        return union(errorMessages, errorMessagesFrom(left));
    }

    private Set<String> evaluateCalls(EvaluationResult result) {
        Set<String> errorMessages = new HashSet<>();
        Set<ExpectedRelation> left = new HashSet<>(Sets.union(expectedConstructorCalls, expectedMethodCalls));
        result.handleViolations((Collection<JavaCall<?>> violatingObjects, String message) ->
                errorMessages.addAll(removeExpectedAccesses(violatingObjects, left)));
        return union(errorMessages, errorMessagesFrom(left));
    }

    private Set<String> evaluateAccesses(EvaluationResult result) {
        Set<String> errorMessages = new HashSet<>();
        Set<ExpectedRelation> left = new HashSet<ExpectedRelation>() {
            {
                addAll(expectedConstructorCalls);
                addAll(expectedMethodCalls);
                addAll(expectedFieldAccesses);
            }
        };
        result.handleViolations((Collection<JavaAccess<?>> violatingObjects, String message) ->
                errorMessages.addAll(removeExpectedAccesses(violatingObjects, left)));
        return union(errorMessages, errorMessagesFrom(left));
    }

    private Set<String> evaluateDependencies(EvaluationResult result) {
        Set<String> errorMessages = new HashSet<>();
        Set<ExpectedRelation> left = new HashSet<>(expectedDependencies);
        result.handleViolations((Collection<Dependency> violatingObjects, String message) ->
                errorMessages.addAll(removeExpectedAccesses(violatingObjects, left)));
        return union(errorMessages, errorMessagesFrom(left));
    }

    private Set<String> removeExpectedAccesses(Collection<?> violatingObjects, Set<? extends ExpectedRelation> left) {
        Object violatingObject = getOnlyElement(violatingObjects);
        for (Iterator<? extends ExpectedRelation> actualMethodCalls = left.iterator(); actualMethodCalls.hasNext(); ) {
            ExpectedRelation next = actualMethodCalls.next();
            if (next.correspondsTo(violatingObject)) {
                actualMethodCalls.remove();
                return emptySet();
            }
        }
        return singleton("Unexpected violation handling: " + violatingObject);
    }

    private Set<String> errorMessagesFrom(Set<?> set) {
        return set.stream().map(Object::toString).map(m -> "Violation not handled: " + m).collect(toSet());
    }

    HandlingAssertion copy() {
        return new HandlingAssertion(expectedFieldAccesses, expectedMethodCalls, expectedConstructorCalls, expectedDependencies);
    }

    static class Result {
        private final Set<String> errorMessages = new TreeSet<>();

        boolean isSuccess() {
            return errorMessages.isEmpty();
        }

        void addAll(Set<String> errorMessages) {
            this.errorMessages.addAll(errorMessages);
        }

        String describe() {
            return Joiner.on(lineSeparator()).join(errorMessages);
        }
    }
}
