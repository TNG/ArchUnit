package com.tngtech.archunit.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaClasses;
import com.tngtech.archunit.lang.OpenArchRule;
import org.junit.runner.Description;

public class ArchRuleExecution extends ArchTestExecution {
    private final Field ruleField;

    public ArchRuleExecution(Class<?> testClass, Field ruleField) {
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

        private static RuleEvaluationException fieldTypeFailure(Field ruleField) {
            String hint = String.format("Only fields of type %s may be annotated with @%s",
                    OpenArchRule.class.getName(), ArchTest.class.getName());
            String problem = String.format("Cannot evaluate @%s on field %s.%s",
                    ArchTest.class.getSimpleName(), ruleField.getDeclaringClass().getName(), ruleField.getName());
            return new RuleEvaluationException(hint + problem);
        }

        abstract Evaluation evaluateOn(JavaClasses classes);

        private static class Retrieved extends RuleToEvaluate {
            private final Field ruleField;
            private OpenArchRule<JavaClass> rule;

            public Retrieved(Field ruleField, OpenArchRule<JavaClass> rule) {
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
        private final Field ruleField;
        private final OpenArchRule<JavaClass> rule;
        private final JavaClasses classes;

        public RetrievalEvaluation(Field ruleField, OpenArchRule<JavaClass> rule, JavaClasses classes) {
            this.ruleField = ruleField;
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

}
