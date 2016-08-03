package com.tngtech.archunit.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

import com.tngtech.archunit.core.JavaClasses;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class ArchTestExecution {
    final Class<?> testClass;

    ArchTestExecution(Class<?> testClass) {
        this.testClass = testClass;
    }

    static <T extends Member> T validate(T member) {
        checkArgument(Modifier.isPublic(member.getModifiers()) && Modifier.isStatic(member.getModifiers()),
                "With @%s annotated members must be public and static", ArchTest.class.getSimpleName());
        return member;
    }

    public abstract Result evaluateOn(JavaClasses classes);

    abstract Description describeSelf();

    @Override
    public String toString() {
        return describeSelf().toString();
    }

    public abstract String getName();

    public abstract <T extends Annotation> T getAnnotation(Class<T> type);

    public boolean ignore() {
        return testClass.getAnnotation(ArchIgnore.class) != null || getAnnotation(ArchIgnore.class) != null;
    }

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
            // Do nothing
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
