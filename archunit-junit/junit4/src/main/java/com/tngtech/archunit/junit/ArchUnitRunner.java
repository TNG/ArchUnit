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
package com.tngtech.archunit.junit;

import java.util.List;

import com.tngtech.archunit.Internal;
import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.MayResolveTypesViaReflection;
import com.tngtech.archunit.base.ReflectionUtils;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import static com.tngtech.archunit.PublicAPI.Usage.ACCESS;

/**
 * Evaluates {@link ArchRule ArchRules} against the classes inside of the packages specified via
 * {@link AnalyzeClasses @AnalyzeClasses} on the annotated test class.
 * <p>
 * NOTE: The runner demands {@link AnalyzeClasses @AnalyzeClasses} to be present on the respective test class.
 * </p>
 * Example:
 * <pre><code>
 *{@literal @}RunWith(ArchUnitRunner.class)
 *{@literal @}AnalyzeClasses(packages = "com.example")
 * public class SomeArchTest {
 *    {@literal @}ArchTest
 *     public static final ArchRule some_rule = //...
 * }
 * </code></pre>
 *
 * The runner will cache classes between test runs, for details please refer to {@link com.tngtech.archunit.junit.internal.ClassCache}.
 */
@PublicAPI(usage = ACCESS)
public class ArchUnitRunner<T> extends ParentRunner<T> {
    private final InternalRunner<T> runnerDelegate;

    @Internal
    public ArchUnitRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
        runnerDelegate = createInternalRunner(testClass);
    }

    private InternalRunner<T> createInternalRunner(Class<?> testClass) {
        Class<InternalRunner<T>> runnerClass = classForName("com.tngtech.archunit.junit.internal.ArchUnitRunnerInternal");
        return ReflectionUtils.newInstanceOf(runnerClass, testClass);
    }

    @SuppressWarnings("unchecked")
    @MayResolveTypesViaReflection(reason = "Only used to load internal ArchUnit class")
    private static <T> Class<T> classForName(String typeName) {
        try {
            return (Class<T>) Class.forName(typeName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected Statement classBlock(RunNotifier notifier) {
        return runnerDelegate.classBlock(notifier);
    }

    @Override
    protected List<T> getChildren() {
        return runnerDelegate.getChildren();
    }

    @Override
    protected Description describeChild(T child) {
        return runnerDelegate.describeChild(child);
    }

    @Override
    protected void runChild(T child, RunNotifier notifier) {
        runnerDelegate.runChild(child, notifier);
    }

    @Internal
    public interface InternalRunner<T> {
        Statement classBlock(RunNotifier notifier);

        List<T> getChildren();

        Description describeChild(T child);

        void runChild(T child, RunNotifier notifier);
    }
}
