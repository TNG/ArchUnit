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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.tngtech.archunit.base.ArchUnitException.ReflectionException;
import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import static com.tngtech.archunit.junit.ReflectionUtils.newInstanceOf;

abstract class ArchTestExecution {
    final Class<?> testClass;
    private final boolean ignore;

    ArchTestExecution(Class<?> testClass, boolean ignore) {
        this.testClass = testClass;
        this.ignore = ignore;
    }

    abstract Result evaluateOn(JavaClasses classes);

    abstract Description describeSelf();

    @Override
    public String toString() {
        return describeSelf().toString();
    }

    abstract String getName();

    boolean ignore() {
        return ignore;
    }

    static <T> T getValue(Field field) {
        try {
            if (Modifier.isStatic(field.getModifiers())) {
                return ReflectionUtils.getValue(field, null);
            } else {
                return ReflectionUtils.getValue(field, newInstanceOf(field.getDeclaringClass()));
            }
        } catch (ReflectionException e) {
            throw new ArchTestInitializationException(e.getCause());
        }
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
