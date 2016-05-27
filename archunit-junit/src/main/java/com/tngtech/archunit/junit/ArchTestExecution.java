package com.tngtech.archunit.junit;

import com.tngtech.archunit.core.JavaClasses;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.TestClass;

public abstract class ArchTestExecution {
    final TestClass testClass;

    ArchTestExecution(TestClass testClass) {
        this.testClass = testClass;
    }

    public Result evaluateOn(JavaClasses classes) {
        if (testClass.getJavaClass().getAnnotation(ArchIgnore.class) != null) {
            return new IgnoredResult(describeSelf());
        }
        return doEvaluateOn(classes);
    }

    abstract Result doEvaluateOn(JavaClasses classes);

    abstract Description describeSelf();

    static abstract class Result {
        abstract void notify(RunNotifier notifier);
    }

    static class PositiveResult extends Result {
        private final Description description;

        PositiveResult(Description description) {
            this.description = description;
        }

        @Override
        void notify(RunNotifier notifier) {
            notifier.fireTestFinished(description);
        }
    }

    static class IgnoredResult extends Result {
        private final Description description;

        IgnoredResult(Description description) {
            this.description = description;
        }

        @Override
        void notify(RunNotifier notifier) {
            notifier.fireTestIgnored(description);
        }
    }

    static class NegativeResult extends Result {
        private final Description description;
        private final Throwable failure;

        NegativeResult(Description description, Throwable failure) {
            this.description = description;
            this.failure = failure;
        }

        @Override
        void notify(RunNotifier notifier) {
            notifier.fireTestFailure(new Failure(description, failure));
        }
    }
}
