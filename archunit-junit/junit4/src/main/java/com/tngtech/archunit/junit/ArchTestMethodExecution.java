/*
 * Copyright 2014-2020 TNG Technology Consulting GmbH
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

import java.lang.reflect.Method;
import java.util.Arrays;

import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.runner.Description;

import static com.tngtech.archunit.junit.ReflectionUtils.invokeMethod;

class ArchTestMethodExecution extends ArchTestExecution {
    private final Method testMethod;

    ArchTestMethodExecution(Class<?> testClass, Method testMethod, boolean ignore) {
        super(testClass, ignore);
        this.testMethod = testMethod;
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

    private void executeTestMethod(JavaClasses classes) {
        ArchTestInitializationException.check(
                Arrays.equals(testMethod.getParameterTypes(), new Class<?>[]{JavaClasses.class}),
                "Methods annotated with @%s must have exactly one parameter of type %s",
                ArchTest.class.getSimpleName(), JavaClasses.class.getSimpleName());

        invokeMethod(testMethod, testClass, classes);
    }

    @Override
    Description describeSelf() {
        return Description.createTestDescription(testClass, testMethod.getName());
    }

    @Override
    String getName() {
        return testMethod.getName();
    }

}
