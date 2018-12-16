package com.tngtech.archunit.testutils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.api.DynamicTest;
import org.junit.platform.runner.JUnitPlatform;
import org.junit.runner.JUnitCore;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.System.lineSeparator;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

public class ExpectedTestFailures {
    private final SortedSet<Class<?>> testClasses;
    private final LinkedList<ExpectedViolationToAssign> expectedViolations = new LinkedList<>();

    private ExpectedTestFailures(Class<?>[] testClasses) {
        this.testClasses = Arrays.stream(testClasses).collect(toCollection(() -> new TreeSet<Class<?>>(comparing(Class::getName))));
    }

    public static ExpectedTestFailures forTests(Class<?>... testClasses) {
        return new ExpectedTestFailures(testClasses);
    }

    public ExpectedTestFailures ofRule(String memberName, String ruleText) {
        expectedViolations.add(ExpectedViolationToAssign.by(memberName, ruleText));
        return this;
    }

    public ExpectedTestFailures ofRule(String ruleText) {
        expectedViolations.add(ExpectedViolationToAssign.byRuleText(ruleText));
        return this;
    }

    public ExpectedTestFailures by(ExpectedAccess.ExpectedFieldAccess access) {
        expectedViolations.getLast().by(access);
        return this;
    }

    public ExpectedTestFailures by(ExpectedAccess.ExpectedCall call) {
        expectedViolations.getLast().by(call);
        return this;
    }

    public ExpectedTestFailures by(ExpectedDependency inheritance) {
        expectedViolations.getLast().by(inheritance);
        return this;
    }

    public ExpectedTestFailures by(MessageAssertionChain.Link assertion) {
        expectedViolations.getLast().by(assertion);
        return this;
    }

    public Stream<DynamicTest> toDynamicTests() {
        return testClasses.stream()
                .map(RunnableTest::from)
                .map(test -> dynamicTest(
                        test.getDisplayName(),
                        () -> assertActualAndExpectedViolationsMatch(test)));
    }

    private void assertActualAndExpectedViolationsMatch(RunnableTest test) {
        RemainingViolations remainingExpected = new RemainingViolations(this.expectedViolations);
        UnexpectedErrors unexpectedErrors = new UnexpectedErrors();
        WrongViolations wrongViolations = new WrongViolations();

        TestFailures testFailures = test.run();
        testFailures.assertNoUnexpectedErrorType();
        for (TestFailure failure : testFailures) {
            Optional<ExpectedViolationToAssign> expectedViolation = remainingExpected.findAndRemoveMatching(failure);
            if (expectedViolation.isPresent()) {
                wrongViolations.addIfWrong(expectedViolation.get().evaluate(failure.getAssertionError()));
            } else {
                unexpectedErrors.add(failure.getAssertionError());
            }
        }

        if (unexpectedErrors.exist() || wrongViolations.exist()) {
            fail(String.format("Test class %s hasn't produced the correct violations:%n%s%n%s",
                    test.testClass.getName(),
                    unexpectedErrors.describe(),
                    wrongViolations.describe()));
        }
        if (!remainingExpected.isEmpty()) {
            fail(String.format("Some expected violations haven't occurred. Expected:%n%s", remainingExpected.describe()));
        }
    }

    public ExpectedTestFailures times(int amount) {
        checkArgument(amount > 1, "Needs an amount > 1 to make sense");
        for (int i = 0; i < amount - 1; i++) {
            expectedViolations.add(expectedViolations.getLast().copy());
        }
        return this;
    }

    private abstract static class RunnableTest {
        final Class<?> testClass;

        RunnableTest(Class<?> testClass) {
            this.testClass = testClass;
        }

        String getDisplayName() {
            return "Expected violations of " + testClass.getName();
        }

        abstract TestFailures run();

        static RunnableTest from(Class<?> testClass) {
            if (testClass.getName().contains(".junit5.")) {
                return new RunnableJUnit5Test(testClass);
            } else {
                return new RunnableJUnit4Test(testClass);
            }
        }
    }

    private static class RunnableJUnit4Test extends RunnableTest {
        private RunnableJUnit4Test(Class<?> testClass) {
            super(testClass);
        }

        @Override
        TestFailures run() {
            List<TestFailure> result = new JUnitCore().run(testClass).getFailures().stream()
                    .map(failure -> new TestFailure(failure, failure.getException()))
                    .collect(toList());
            return new TestFailures(result);
        }
    }

    private static class RunnableJUnit5Test extends RunnableTest {
        private RunnableJUnit5Test(Class<?> testClass) {
            super(testClass);
        }

        @Override
        TestFailures run() {
            List<TestFailure> result = new ArrayList<>();
            new JUnitPlatform(testClass).run(new RunNotifier() {
                @Override
                public void fireTestFailure(Failure failure) {
                    result.add(new TestFailure(failure, failure.getException()));
                }
            });
            return new TestFailures(result);
        }
    }

    private static class RemainingViolations {
        private final LinkedList<ExpectedViolationToAssign> remainingExpected;

        RemainingViolations(List<ExpectedViolationToAssign> expectedViolations) {
            this.remainingExpected = new LinkedList<>(expectedViolations);
        }

        Optional<ExpectedViolationToAssign> findAndRemoveMatching(TestFailure failure) {
            Optional<ExpectedViolationToAssign> match = remainingExpected.stream()
                    .filter(expectedViolation -> expectedViolation.isAssignedTo(failure))
                    .findFirst();
            match.ifPresent(remainingExpected::remove);
            return match;
        }

        boolean isEmpty() {
            return remainingExpected.isEmpty();
        }

        String describe() {
            return remainingExpected.stream().map(ExpectedViolationToAssign::describe).collect(joining(lineSeparator()));
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + describe();
        }
    }

    private static class UnexpectedErrors {
        List<AssertionError> unexpectedErrors = new ArrayList<>();

        void add(AssertionError error) {
            unexpectedErrors.add(error);
        }

        boolean exist() {
            return !unexpectedErrors.isEmpty();
        }

        String describe() {
            return exist() ?
                    "Unexpected errors: " +
                            unexpectedErrors.stream()
                                    .map(AssertionError::getMessage)
                                    .collect(joining(lineSeparator())) :
                    "";
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + unexpectedErrors;
        }
    }

    private static class WrongViolations {
        List<ExpectedViolationToAssign.Result> comparisonResults = new ArrayList<>();

        void addIfWrong(ExpectedViolationToAssign.Result result) {
            result.ifFailure(r -> comparisonResults.add(r));
        }

        boolean exist() {
            return !comparisonResults.isEmpty();
        }

        String describe() {
            return exist() ?
                    "Test has not failed as expected: " + lineSeparator() +
                            comparisonResults.stream()
                                    .map(ExpectedViolationToAssign.Result::describe)
                                    .collect(joining(lineSeparator())) :
                    "";
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + comparisonResults;
        }
    }

    private static class TestFailure {
        final String memberName;
        final Throwable error;

        private TestFailure(Failure junitFailure, Throwable error) {
            this.memberName = junitFailure.getDescription().getMethodName();
            this.error = error;
        }

        AssertionError getAssertionError() {
            return (AssertionError) error;
        }

        @Override
        public String toString() {
            return "TestFailure{" +
                    "memberName='" + memberName + '\'' +
                    ", error=" + error +
                    '}';
        }
    }

    private static class TestFailures implements Iterable<TestFailure> {
        private final List<TestFailure> failures;

        private TestFailures(Iterable<TestFailure> failures) {
            this.failures = ImmutableList.copyOf(failures);
        }

        void assertNoUnexpectedErrorType() {
            List<TestFailure> unexpectedFailureTypes = failures.stream()
                    .filter(f -> !(f.error instanceof AssertionError))
                    .collect(toList());

            if (!unexpectedFailureTypes.isEmpty()) {
                throw new AssertionError(String.format(
                        "Some failures were due to an unexpected error (i.e. not %s): %s",
                        AssertionError.class.getName(),
                        unexpectedFailureTypes));
            }
        }

        @Override
        public Iterator<TestFailure> iterator() {
            return failures.iterator();
        }
    }

    private static class ExpectedViolationToAssign {
        private final Predicate<TestFailure> failurePredicate;
        private final ExpectedViolation expectedViolation;
        private final HandlingAssertion handlingAssertion;

        static ExpectedViolationToAssign byRuleText(String ruleText) {
            return new ExpectedViolationToAssign(
                    failure -> failure.error.getMessage().contains(String.format("Rule '%s'", ruleText)),
                    ExpectedViolation.ofRule(ruleText),
                    HandlingAssertion.ofRule());
        }

        static ExpectedViolationToAssign by(String memberName, String ruleText) {
            return new ExpectedViolationToAssign(
                    failure -> failure.memberName.equals(memberName),
                    ExpectedViolation.ofRule(ruleText),
                    HandlingAssertion.ofRule());
        }

        private ExpectedViolationToAssign(
                Predicate<TestFailure> failurePredicate,
                ExpectedViolation expectedViolation,
                HandlingAssertion handlingAssertion) {

            this.failurePredicate = failurePredicate;
            this.expectedViolation = expectedViolation;
            this.handlingAssertion = handlingAssertion;
        }

        String describe() {
            return expectedViolation.describe();
        }

        boolean isAssignedTo(TestFailure failure) {
            return failurePredicate.test(failure);
        }

        void by(ExpectedAccess.ExpectedFieldAccess access) {
            expectedViolation.by(access);
            handlingAssertion.by(access);
        }

        void by(ExpectedAccess.ExpectedCall call) {
            expectedViolation.by(call);
            handlingAssertion.by(call);
        }

        void by(ExpectedDependency inheritance) {
            expectedViolation.by(inheritance);
            handlingAssertion.by(inheritance);
        }

        void by(MessageAssertionChain.Link assertion) {
            expectedViolation.by(assertion);
        }

        ExpectedViolationToAssign copy() {
            return new ExpectedViolationToAssign(failurePredicate, expectedViolation.copy(), handlingAssertion.copy());
        }

        Result evaluate(AssertionError error) {
            ViolationComparisonResult violationResult = expectedViolation.evaluate(error);
            EvaluationResult evaluationResult = ResultStoringExtension.getEvaluationResultFor(error.getMessage());
            HandlingAssertion.Result handlingResult = handlingAssertion.evaluate(evaluationResult);
            return new Result(violationResult, handlingResult);
        }

        static class Result {
            private final ViolationComparisonResult violationResult;
            private final HandlingAssertion.Result handlingResult;

            Result(ViolationComparisonResult violationResult, HandlingAssertion.Result handlingResult) {
                this.violationResult = violationResult;
                this.handlingResult = handlingResult;
            }

            boolean isSuccess() {
                return violationResult.isSuccess() && handlingResult.isSuccess();
            }

            void ifFailure(Consumer<Result> doWithResult) {
                if (!isSuccess()) {
                    doWithResult.accept(this);
                }
            }

            String describe() {
                Joiner joiner = Joiner.on(lineSeparator()).skipNulls();
                String violationResultDescription =
                        violationResult.isSuccess()
                                ? null
                                : joiner.join("Result from comparing violations: ", violationResult.describe());
                String handlingResultDescription =
                        handlingResult.isSuccess()
                                ? null
                                : joiner.join("Result from handling violations: ", handlingResult.describe());
                return joiner.join(violationResultDescription, handlingResultDescription);
            }
        }
    }
}
