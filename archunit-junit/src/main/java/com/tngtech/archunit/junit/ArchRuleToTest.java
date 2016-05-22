package com.tngtech.archunit.junit;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.lang.OpenArchRule;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkField;
import org.junit.runners.model.TestClass;

public class ArchRuleToTest {
    private final TestClass testClass;
    private final FrameworkField ruleField;

    public ArchRuleToTest(TestClass testClass, FrameworkField ruleField) {
        this.testClass = testClass;
        this.ruleField = ruleField;
    }

    public FrameworkField getField() {
        return ruleField;
    }

    public Result evaluateOn(JavaClasses classes) {
        if (testClass.getJavaClass().getAnnotation(ArchIgnore.class) != null) {
            return new IgnoredResult(describeSelf());
        }
        return RuleToEvaluate.from(ruleField, testClass.getJavaClass())
                .evaluateOn(classes)
                .asResult(describeSelf());
    }

    public Description describeSelf() {
        return Description.createTestDescription(testClass.getJavaClass(), ruleField.getField().getName());
    }

    private static abstract class RuleToEvaluate {
        static RuleToEvaluate retrievalFailedWith(RuleEvaluationException exception) {
            return new FailedToRetrieve(exception);
        }

        static RuleToEvaluate from(FrameworkField ruleField, Class<?> testClass) {
            return getRule(ruleField, testClass);
        }

        private static RuleToEvaluate getRule(FrameworkField ruleField, Class<?> testClass) {
            try {
                Object ruleCandidate = ruleField.get(testClass);
                return ruleCandidate instanceof OpenArchRule ?
                        new Retrieved(ruleField, asArchRule(ruleCandidate)) :
                        new FailedToRetrieve(fieldTypeFailure(ruleField));
            } catch (IllegalAccessException e) {
                return RuleToEvaluate.retrievalFailedWith(new RuleEvaluationException(
                        String.format("Cannot access field %s.%s", testClass.getName(), ruleField.getName()), e));
            }
        }

        @SuppressWarnings("unchecked")
        private static OpenArchRule<JavaClass> asArchRule(Object ruleCandidate) {
            return (OpenArchRule<JavaClass>) ruleCandidate;
        }

        private static RuleEvaluationException fieldTypeFailure(FrameworkField ruleField) {
            String hint = String.format("Only fields of type %s may be annotated with @%s",
                    OpenArchRule.class.getName(), ArchTest.class.getName());
            String problem = String.format("Cannot evaluate @%s on field %s.%s",
                    ArchTest.class.getSimpleName(), ruleField.getDeclaringClass().getName(), ruleField.getName());
            return new RuleEvaluationException(hint + problem);
        }

        abstract Evaluation evaluateOn(JavaClasses classes);

        private static class Retrieved extends RuleToEvaluate {
            private final FrameworkField ruleField;
            private OpenArchRule<JavaClass> rule;

            public Retrieved(FrameworkField ruleField, OpenArchRule<JavaClass> rule) {
                this.ruleField = ruleField;
                this.rule = rule;
            }

            @Override
            public Evaluation evaluateOn(JavaClasses classes) {
                return new RetrievalEvaluation(ruleField, rule, classes);
            }
        }

        private static class FailedToRetrieve extends RuleToEvaluate {
            private RuleEvaluationException failure;

            public FailedToRetrieve(RuleEvaluationException failure) {
                this.failure = failure;
            }

            @Override
            public Evaluation evaluateOn(JavaClasses classes) {
                return new FailureEvaluation(failure);
            }
        }
    }

    private static abstract class Evaluation {
        abstract Result asResult(Description description);
    }

    private static class RetrievalEvaluation extends Evaluation {
        private final FrameworkField ruleField;
        private final OpenArchRule<JavaClass> rule;
        private final JavaClasses classes;

        public RetrievalEvaluation(FrameworkField ruleField, OpenArchRule<JavaClass> rule, JavaClasses classes) {
            this.ruleField = ruleField;
            this.rule = rule;
            this.classes = classes;
        }

        @Override
        Result asResult(Description description) {
            if (ruleField.getField().getAnnotation(ArchIgnore.class) != null) {
                return new IgnoredResult(description);
            }
            try {
                rule.check(classes);
            } catch (Exception | AssertionError e) {
                return new NegativeResult(description, e);
            }
            return new PositiveResult(description);
        }
    }

    private static class FailureEvaluation extends Evaluation {
        private final RuleEvaluationException failure;

        public FailureEvaluation(RuleEvaluationException failure) {
            this.failure = failure;
        }

        @Override
        Result asResult(Description description) {
            return new NegativeResult(description, failure);
        }
    }

    public static abstract class Result {
        public abstract void notify(RunNotifier notifier);
    }

    public static class PositiveResult extends Result {
        private final Description description;

        public PositiveResult(Description description) {
            this.description = description;
        }

        @Override
        public void notify(RunNotifier notifier) {
            notifier.fireTestFinished(description);
        }
    }

    public static class IgnoredResult extends Result {
        private final Description description;

        public IgnoredResult(Description description) {
            this.description = description;
        }

        @Override
        public void notify(RunNotifier notifier) {
            notifier.fireTestIgnored(description);
        }
    }

    public static class NegativeResult extends Result {
        private final Description description;
        private final Throwable failure;

        public NegativeResult(Description description, Throwable failure) {
            this.description = description;
            this.failure = failure;
        }

        @Override
        public void notify(RunNotifier notifier) {
            notifier.fireTestFailure(new Failure(description, failure));
        }
    }
}
