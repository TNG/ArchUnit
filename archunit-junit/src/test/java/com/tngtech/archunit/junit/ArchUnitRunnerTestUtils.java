package com.tngtech.archunit.junit;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.AbstractArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import org.junit.runners.model.InitializationError;

public class ArchUnitRunnerTestUtils {
    static final AbstractArchCondition<JavaClass> ALWAYS_SATISFIED = new AbstractArchCondition<JavaClass>() {
        @Override
        public void check(JavaClass item, ConditionEvents events) {
            events.add(ConditionEvent.satisfied("I'm always satisfied"));
        }
    };

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
