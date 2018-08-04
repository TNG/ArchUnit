/*
 * Copyright 2018 TNG Technology Consulting GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.tngtech.archunit.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;

import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

abstract class ArchTestExecution {
    final Class<?> testClass;
    private final boolean ignore;

    ArchTestExecution(Class<?> testClass, boolean ignore) {
        this.testClass = testClass;
        this.ignore = ignore;
    }

    static <T extends Member> T validateStatic(T member) {
        ArchTestInitializationException.check(
                Modifier.isStatic(member.getModifiers()),
                "With @%s annotated members must be static", ArchTest.class.getSimpleName());
        return member;
    }

    abstract Result evaluateOn(JavaClasses classes);

    abstract Description describeSelf();

    @Override
    public String toString() {
        return describeSelf().toString();
    }

    abstract String getName();

    abstract <T extends Annotation> T getAnnotation(Class<T> type);

    boolean ignore() {
        return ignore;
    }

    abstract static class Result {
        abstract void notify(RunNotifier notifier);
    }

    static class PositiveResult extends Result {
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
