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
import java.lang.reflect.Method;
import java.util.Arrays;

import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;

class ArchTestMethodExecution extends ArchTestExecution {
    private final Method testMethod;

    ArchTestMethodExecution(Class<?> testClass, Method testMethod, boolean forceIgnore) {
        super(testClass, testMethod, forceIgnore);
        this.testMethod = validatePublicStatic(testMethod);
    }

    @Override
    Result evaluateOn(JavaClasses classes) {
        try {
            executeTestMethod(classes);
            return new PositiveResult();
        } catch (Throwable failure) {
            return new NegativeResult(describeSelf(), failure);
        }
    }

    private void executeTestMethod(JavaClasses classes) throws Throwable {
        if (!Arrays.equals(testMethod.getParameterTypes(), new Class<?>[]{JavaClasses.class})) {
            throw new IllegalArgumentException(String.format(
                    "Methods annotated with @%s must have exactly one parameter of type %s",
                    ArchTest.class.getSimpleName(), JavaClasses.class.getSimpleName()));
        }

        new FrameworkMethod(testMethod).invokeExplosively(testClass.getDeclaredConstructor().newInstance(), classes);
    }

    @Override
    Description describeSelf() {
        return Description.createTestDescription(testClass, testMethod.getName());
    }

    @Override
    String getName() {
        return testMethod.getName();
    }

    @Override
    <T extends Annotation> T getAnnotation(Class<T> type) {
        return testMethod.getAnnotation(type);
    }
}
