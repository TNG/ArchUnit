package com.tngtech.archunit.junit;

import java.util.Arrays;

import com.tngtech.archunit.core.JavaClasses;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

public class ArchTestMethodExecution extends ArchTestExecution {
    private final FrameworkMethod testMethod;

    public ArchTestMethodExecution(TestClass testClass, FrameworkMethod testMethod) {
        super(testClass);
        this.testMethod = testMethod;
    }

    @Override
    Result doEvaluateOn(JavaClasses classes) {
        if (testMethod.getMethod().getAnnotation(ArchIgnore.class) != null) {
            return new IgnoredResult(describeSelf());
        }
        try {
            executeTestMethod(classes);
            return new PositiveResult(describeSelf());
        } catch (Throwable failure) {
            return new NegativeResult(describeSelf(), failure);
        }
    }

    private void executeTestMethod(JavaClasses classes) throws Throwable {
        if (!Arrays.equals(testMethod.getMethod().getParameterTypes(), new Class<?>[]{JavaClasses.class})) {
            throw new IllegalArgumentException(String.format(
                    "Methods annotated with @%s must have exactly one parameter of type %s",
                    ArchTest.class.getSimpleName(), JavaClasses.class.getSimpleName()));
        }

        testMethod.invokeExplosively(testClass.getJavaClass().newInstance(), classes);
    }

    @Override
    public Description describeSelf() {
        return Description.createTestDescription(testClass.getJavaClass(), testMethod.getName());
    }

    public FrameworkMethod getTestMethod() {
        return testMethod;
    }
}
