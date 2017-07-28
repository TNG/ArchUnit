package com.tngtech.archunit.junit;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.Sets;
import com.tngtech.archunit.core.domain.JavaAccess;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.junit.ExpectedAccess.ExpectedCall;
import com.tngtech.archunit.junit.ExpectedAccess.ExpectedFieldAccess;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.ViolationHandler;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static com.google.common.collect.Iterables.getOnlyElement;

class HandlingAssertion implements ExpectsViolations, TestRule {
    private final Set<ExpectedFieldAccess> expectedFieldAccesses = new HashSet<>();
    private final Set<ExpectedCall> expectedMethodCalls = new HashSet<>();
    private final Set<ExpectedCall> expectedConstructorCalls = new HashSet<>();

    @Override
    public ExpectsViolations ofRule(String ruleText) {
        return this;
    }

    @Override
    public ExpectsViolations by(ExpectedFieldAccess access) {
        expectedFieldAccesses.add(access);
        return this;
    }

    @Override
    public ExpectsViolations by(ExpectedCall call) {
        if (call.isToConstructor()) {
            expectedConstructorCalls.add(call);
        } else {
            expectedMethodCalls.add(call);
        }
        return this;
    }

    @Override
    public ExpectsViolations by(MessageAssertionChain.Link assertion) {
        return this;
    }

    static HandlingAssertion none() {
        return new HandlingAssertion();
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return base;
    }

    void assertResult(EvaluationResult result) {
        checkFieldAccesses(result);
        checkMethodCalls(result);
        checkConstructorCalls(result);
        checkCalls(result);
        checkAccesses(result);
    }

    // Too bad Java erases types, otherwise a lot of boilerplate could be avoided here :-(
    // This way we must write explicitly ViolationHandler<ConcreteType> or the bound wont work correctly
    private void checkFieldAccesses(EvaluationResult result) {
        final Set<ExpectedFieldAccess> left = new HashSet<>(this.expectedFieldAccesses);
        result.handleViolations(new ViolationHandler<JavaFieldAccess>() {
            @Override
            public void handle(Collection<JavaFieldAccess> violatingObjects, String message) {
                removeExpectedAccesses(violatingObjects, left);
            }
        });
        checkEmpty(left);
    }

    private void checkMethodCalls(EvaluationResult result) {
        final Set<ExpectedCall> left = new HashSet<>(expectedMethodCalls);
        result.handleViolations(new ViolationHandler<JavaMethodCall>() {
            @Override
            public void handle(Collection<JavaMethodCall> violatingObjects, String message) {
                removeExpectedAccesses(violatingObjects, left);
            }
        });
        checkEmpty(left);
    }

    private void checkConstructorCalls(EvaluationResult result) {
        final Set<ExpectedCall> left = new HashSet<>(expectedConstructorCalls);
        result.handleViolations(new ViolationHandler<JavaConstructorCall>() {
            @Override
            public void handle(Collection<JavaConstructorCall> violatingObjects, String message) {
                removeExpectedAccesses(violatingObjects, left);
            }
        });
        checkEmpty(left);
    }

    private void checkCalls(EvaluationResult result) {
        final Set<ExpectedCall> left = new HashSet<>(Sets.union(expectedConstructorCalls, expectedMethodCalls));
        result.handleViolations(new ViolationHandler<JavaCall<?>>() {
            @Override
            public void handle(Collection<JavaCall<?>> violatingObjects, String message) {
                removeExpectedAccesses(violatingObjects, left);
            }
        });
        checkEmpty(left);
    }

    private void checkAccesses(EvaluationResult result) {
        final Set<ExpectedAccess> left = new HashSet<ExpectedAccess>() {
            {
                addAll(expectedConstructorCalls);
                addAll(expectedMethodCalls);
                addAll(expectedFieldAccesses);
            }
        };
        result.handleViolations(new ViolationHandler<JavaAccess<?>>() {
            @Override
            public void handle(Collection<JavaAccess<?>> violatingObjects, String message) {
                removeExpectedAccesses(violatingObjects, left);
            }
        });
        checkEmpty(left);
    }

    private void removeExpectedAccesses(Collection<? extends JavaAccess<?>> violatingObjects, Set<? extends ExpectedAccess> left) {
        JavaAccess<?> violatingObject = getOnlyElement(violatingObjects);
        for (Iterator<? extends ExpectedAccess> actualMethodCalls = left.iterator(); actualMethodCalls.hasNext(); ) {
            ExpectedAccess next = actualMethodCalls.next();
            if (next.matches(violatingObject)) {
                actualMethodCalls.remove();
                return;
            }
        }
        throw new RuntimeException("Unexpected access: " + violatingObject); // NOTE: Don't throw AssertionError, since that will be caught and analyzed
    }

    private void checkEmpty(Set<?> set) {
        if (!set.isEmpty()) {
            throw new RuntimeException("Unhandled accesses: " + set); // NOTE: Don't throw AssertionError, since that will be caught and analyzed
        }
    }
}
