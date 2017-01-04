package com.tngtech.archunit.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.Description;

public class ArchRuleExecution extends ArchTestExecution {
    private final Field ruleField;

    ArchRuleExecution(Class<?> testClass, Field ruleField) {
        super(testClass);
        this.ruleField = validate(ruleField);
    }

    @Override
    public Result evaluateOn(JavaClasses classes) {
        return RuleToEvaluate.from(testClass, ruleField)
                .evaluateOn(classes)
                .asResult(describeSelf());
    }

    @Override
    public Description describeSelf() {
        return Description.createTestDescription(testClass, ruleField.getName());
    }

    @Override
    public String getName() {
        return ruleField.getName();
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> type) {
        return ruleField.getAnnotation(type);
    }

    private static abstract class RuleToEvaluate {
        static RuleToEvaluate retrievalFailedWith(RuleEvaluationException exception) {
            return new FailedToRetrieve(exception);
        }

        static RuleToEvaluate from(Class<?> testClass, Field ruleField) {
            try {
                Object ruleCandidate = ruleField.get(testClass);
                return ruleCandidate instanceof ArchRule ?
                        new Retrieved(asArchRule(ruleCandidate)) :
                        new FailedToRetrieve(fieldTypeFailure(ruleField));
            } catch (IllegalAccessException e) {
                return RuleToEvaluate.retrievalFailedWith(new RuleEvaluationException(
                        String.format("Cannot access field %s.%s", testClass.getName(), ruleField.getName()), e));
            }
        }

        @SuppressWarnings("unchecked")
        private static ArchRule asArchRule(Object ruleCandidate) {
            return (ArchRule) ruleCandidate;
        }

        private static RuleEvaluationException fieldTypeFailure(Field ruleField) {
            String hint = String.format("Only fields of type %s may be annotated with @%s",
                    ArchRule.class.getName(), ArchTest.class.getName());
            String problem = String.format("Cannot evaluate @%s on field %s.%s",
                    ArchTest.class.getSimpleName(), ruleField.getDeclaringClass().getName(), ruleField.getName());
            return new RuleEvaluationException(hint + problem);
        }

        abstract Evaluation evaluateOn(JavaClasses classes);

        private static class Retrieved extends RuleToEvaluate {
            private ArchRule rule;

            Retrieved(ArchRule rule) {
                this.rule = rule;
            }

            @Override
            public Evaluation evaluateOn(JavaClasses classes) {
                return new RetrievalEvaluation(rule, classes);
            }
        }

        private static class FailedToRetrieve extends RuleToEvaluate {
            private RuleEvaluationException failure;

            FailedToRetrieve(RuleEvaluationException failure) {
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
        private final ArchRule rule;
        private final JavaClasses classes;

        RetrievalEvaluation(ArchRule rule, JavaClasses classes) {
            this.rule = rule;
            this.classes = classes;
        }

        @Override
        Result asResult(Description description) {
            try {
                rule.check(classes);
            } catch (Exception | AssertionError e) {
                return new NegativeResult(description, e);
            }
            return new PositiveResult();
        }
    }

    private static class FailureEvaluation extends Evaluation {
        private final RuleEvaluationException failure;

        FailureEvaluation(RuleEvaluationException failure) {
            this.failure = failure;
        }

        @Override
        Result asResult(Description description) {
            return new NegativeResult(description, failure);
        }
    }

}
