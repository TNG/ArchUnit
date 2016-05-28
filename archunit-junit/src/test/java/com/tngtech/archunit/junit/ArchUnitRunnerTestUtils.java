package com.tngtech.archunit.junit;

import com.google.common.base.Predicates;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import org.junit.runners.model.InitializationError;

public class ArchUnitRunnerTestUtils {
    static final ArchCondition<JavaClass> ALWAYS_SATISFIED = ArchCondition
            .violationIf(Predicates.<JavaClass>alwaysFalse())
            .withMessage("I'm always satisfied");

    static ArchUnitRunner newRunnerFor(Class<?> testClass) {
        try {
            return new ArchUnitRunner(testClass);
        } catch (InitializationError initializationError) {
            throw new RuntimeException(initializationError);
        }
    }

    static ArchTestExecution getRule(String name, ArchUnitRunner runner) {
        for (ArchTestExecution ruleToTest : runner.getChildren()) {
            if (name.equals(ruleToTest.getName())) {
                return ruleToTest;
            }
        }
        throw new RuntimeException(String.format("Couldn't find Rule with name '%s'", name));
    }
}
