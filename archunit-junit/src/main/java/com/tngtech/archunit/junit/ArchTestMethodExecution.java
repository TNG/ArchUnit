package com.tngtech.archunit.junit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

import com.tngtech.archunit.core.domain.JavaClasses;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;

class ArchTestMethodExecution extends ArchTestExecution {
    private final Method testMethod;

    ArchTestMethodExecution(Class<?> testClass, Method testMethod) {
        super(testClass);
        this.testMethod = validate(testMethod);
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

        new FrameworkMethod(testMethod).invokeExplosively(testClass.newInstance(), classes);
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
