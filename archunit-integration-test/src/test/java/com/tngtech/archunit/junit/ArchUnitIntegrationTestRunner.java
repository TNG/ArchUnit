package com.tngtech.archunit.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.integration.junit.ExpectedViolationFrom;
import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import static com.google.common.base.Preconditions.checkNotNull;

public class ArchUnitIntegrationTestRunner extends ArchUnitRunner {
    public ArchUnitIntegrationTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected void runChild(final ArchTestExecution child, final RunNotifier notifier) {
        ExpectedViolation expectedViolation = ExpectedViolation.none();
        HandlingAssertion handlingAssertion = HandlingAssertion.none();
        Description description = describeChild(child);
        notifier.fireTestStarted(description);
        try {
            ExpectedViolationDefinition violationDefinition = extractExpectedConfiguration(child);
            violationDefinition.configure(expectedViolation);
            violationDefinition.configure(handlingAssertion);
            expectedViolation.apply(new IntegrationTestStatement(child, handlingAssertion), description).evaluate();
        } catch (Throwable throwable) {
            notifier.fireTestFailure(new Failure(description, throwable));
        } finally {
            notifier.fireTestFinished(description);
        }
    }

    private ExpectedViolationDefinition extractExpectedConfiguration(ArchTestExecution child) {
        ExpectedViolationFrom annotation = child.getAnnotation(ExpectedViolationFrom.class);
        if (annotation == null) {
            throw new RuntimeException("IntegrationTests need to annotate their @"
                    + ArchTest.class.getSimpleName() + "'s with @" + ExpectedViolationFrom.class.getSimpleName());
        }
        return new ExpectedViolationDefinition(annotation);
    }

    private class IntegrationTestStatement extends Statement {
        private final ArchRuleExecution child;
        private final HandlingAssertion handlingAssertion;

        IntegrationTestStatement(ArchTestExecution child, HandlingAssertion handlingAssertion) {
            this.child = (ArchRuleExecution) child;
            this.handlingAssertion = handlingAssertion;
        }

        @Override
        public void evaluate() throws Throwable {
            FailureSniffer sniffer = new FailureSniffer();
            ArchUnitIntegrationTestRunner.super.runChild(child, sniffer);
            checkHandling();
            sniffer.rethrowIfFailure();
        }

        private void checkHandling() {
            ClassesCaptor classesCaptor = new ClassesCaptor();
            ArchUnitIntegrationTestRunner.super.runChild(classesCaptor, new RunNotifier());
            EvaluationResult result = child.rule.evaluate(classesCaptor.getClasses());
            handlingAssertion.assertResult(result);
        }
    }

    private static class FailureSniffer extends RunNotifier {
        private Throwable exception;

        @Override
        public void fireTestFailure(Failure failure) {
            exception = failure.getException();
        }

        void rethrowIfFailure() throws Throwable {
            if (exception != null) {
                throw exception;
            }
        }
    }

    private static class ExpectedViolationDefinition {
        private final Class<?> location;
        private final String method;

        ExpectedViolationDefinition(ExpectedViolationFrom annotation) {
            location = annotation.location();
            method = annotation.method();
        }

        void configure(ExpectsViolations expectsViolations) {
            try {
                Method expectViolation = location.getDeclaredMethod(method, ExpectsViolations.class);
                expectViolation.setAccessible(true);
                expectViolation.invoke(null, expectsViolations);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Cannot find method '" + method + "' on " + location.getSimpleName(), e);
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException("Can't call method '" + method + "' on " + location.getSimpleName(), e);
            }
        }
    }

    private static class ClassesCaptor extends ArchTestExecution {
        private JavaClasses classes;

        ClassesCaptor() {
            super(Object.class, false);
        }

        @Override
        Result evaluateOn(JavaClasses classes) {
            this.classes = classes;
            return new Result() {
                @Override
                void notify(RunNotifier notifier) {
                }
            };
        }

        @Override
        Description describeSelf() {
            return null;
        }

        @Override
        String getName() {
            return null;
        }

        @Override
        <T extends Annotation> T getAnnotation(Class<T> type) {
            return null;
        }

        JavaClasses getClasses() {
            return checkNotNull(classes, "Couldn't retrieve any classes from evaluation");
        }
    }
}
