/*
 * Copyright 2014-2025 TNG Technology Consulting GmbH
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
package com.tngtech.archunit.junit.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.runner.Description;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import static com.tngtech.archunit.junit.internal.DisplayNameResolver.determineDisplayName;
import static com.tngtech.archunit.junit.internal.ReflectionUtils.getValueOrThrowException;
import static java.util.stream.Collectors.joining;

abstract class ArchTestExecution {
    final List<Class<?>> testClassPath;
    final Class<?> ruleDeclaringClass;
    private final boolean ignore;

    ArchTestExecution(List<Class<?>> testClassPath, Class<?> ruleDeclaringClass, boolean ignore) {
        this.testClassPath = testClassPath;
        this.ruleDeclaringClass = ruleDeclaringClass;
        this.ignore = ignore;
    }

    abstract Result evaluateOn(JavaClasses classes);

    abstract Description describeSelf();

    @Override
    public String toString() {
        return describeSelf().toString();
    }

    <T extends Member & AnnotatedElement> Description createDescription(T member) {
        Annotation[] annotations = Stream.concat(
                Arrays.stream(member.getAnnotations()),
                Stream.of(new ArchTestMetaInfo.Instance(member.getName()))
        ).toArray(Annotation[]::new);
        String testName = formatWithPath(member.getName());
        return Description.createTestDescription(testClassPath.get(0), determineDisplayName(testName), annotations);
    }

    private String formatWithPath(String testName) {
        if (testClassPath.size() <= 1) {
            return testName;
        }

        return Stream.concat(
                testClassPath.subList(1, testClassPath.size()).stream().map(Class::getSimpleName),
                Stream.of(testName)
        ).collect(joining(" > "));
    }

    abstract String getName();

    boolean ignore() {
        return ignore;
    }

    static <T> T getValue(Field field, Class<?> fieldOwner) {
        return getValueOrThrowException(field, fieldOwner, ArchTestInitializationException::new);
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
