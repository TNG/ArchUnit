package com.tngtech.archunit.integration;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.tngtech.archunit.integration.CodingRulesWithRunnerIntegrationTest.ExpectedViolationFrom;
import com.tngtech.archunit.junit.ArchRuleToTest;
import com.tngtech.archunit.junit.ArchTest;
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
    protected void runChild(final ArchRuleToTest child, final RunNotifier notifier) {
        expectedViolation = ExpectedViolation.none();
        Description description = describeChild(child);
        notifier.fireTestStarted(description);
        try {
            configureExpectedViolationFor(child);
            expectedViolation.apply(new IntegrationTestStatement(child), description).evaluate();
            notifier.fireTestFinished(description);
        } catch (Throwable throwable) {
            notifier.fireTestFailure(new Failure(description, throwable));
        }
    }

    private void configureExpectedViolationFor(ArchRuleToTest child) {
        String expectationConfiguration = extractConfigurationMethodName(child);
        try {
            Method method = CodingRulesIntegrationTest.class.getDeclaredMethod(expectationConfiguration, ExpectedViolation.class);
            method.invoke(null, expectedViolation);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Cannot find method '" + expectationConfiguration + "' on "
                    + CodingRulesIntegrationTest.class.getSimpleName());
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException("Can't call method '" + expectationConfiguration + "' on "
                    + CodingRulesIntegrationTest.class.getSimpleName());
        }
    }

    private String extractConfigurationMethodName(ArchRuleToTest child) {
        ExpectedViolationFrom annotation = child.getField().getAnnotation(ExpectedViolationFrom.class);
        if (annotation == null) {
            throw new RuntimeException("IntegrationTests need to annotate their @"
                    + ArchTest.class.getSimpleName() + "'s with @" + ExpectedViolationFrom.class.getSimpleName());
        }
        return annotation.value();
    }

    private class IntegrationTestStatement extends Statement {
        private final ArchRuleToTest child;

        public IntegrationTestStatement(ArchRuleToTest child) {
            this.child = child;
        }

        @Override
        public void evaluate() throws Throwable {
            FailureSniffer sniffer = new FailureSniffer();
            ArchUnitIntegrationTestRunner.super.runChild(child, sniffer);
            sniffer.rethrow();
        }
    }

    private static class FailureSniffer extends RunNotifier {
        private Throwable exception;

        @Override
        public void fireTestFailure(Failure failure) {
            exception = failure.getException();
        }

        void rethrow() throws Throwable {
            throw exception;
        }
    }
}
