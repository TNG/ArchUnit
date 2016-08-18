package com.tngtech.archunit.integration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.tngtech.archunit.junit.ArchRuleExecution;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTestExecution;
import com.tngtech.archunit.junit.ArchUnitRunner;
import com.tngtech.archunit.junit.ExpectedViolation;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

public class ArchUnitIntegrationTestRunner extends ArchUnitRunner {
    private ExpectedViolation expectedViolation;

    public ArchUnitIntegrationTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected void runChild(final ArchTestExecution child, final RunNotifier notifier) {
        expectedViolation = ExpectedViolation.none();
        Description description = describeChild(child);
        notifier.fireTestStarted(description);
        try {
            extractExpectedConfiguration(child).configure(expectedViolation);
            expectedViolation.apply(new IntegrationTestStatement(child), description).evaluate();
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

        public IntegrationTestStatement(ArchTestExecution child) {
            this.child = (ArchRuleExecution) child;
        }

        @Override
        public void evaluate() throws Throwable {
            FailureSniffer sniffer = new FailureSniffer();
            ArchUnitIntegrationTestRunner.super.runChild(child, sniffer);
            sniffer.rethrowIfFailure();
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

        public ExpectedViolationDefinition(ExpectedViolationFrom annotation) {
            location = annotation.location();
            method = annotation.method();
        }

        public void configure(ExpectedViolation expectedViolation) {
            try {
                Method expectViolation = location.getDeclaredMethod(method, ExpectedViolation.class);
                expectViolation.setAccessible(true);
                expectViolation.invoke(null, expectedViolation);
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Cannot find method '" + method + "' on " + location.getSimpleName());
            } catch (InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException("Can't call method '" + method + "' on " + location.getSimpleName());
            }
        }
    }
}
