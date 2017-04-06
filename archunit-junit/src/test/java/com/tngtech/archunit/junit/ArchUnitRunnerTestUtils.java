package com.tngtech.archunit.junit;

import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.runners.model.InitializationError;

import static com.tngtech.archunit.lang.conditions.ArchConditions.never;

class ArchUnitRunnerTestUtils {
    static final ArchCondition<JavaClass> BE_SATISFIED = new ArchCondition<JavaClass>("satisfy something") {
        @Override
        public void check(JavaClass item, ConditionEvents events) {
            events.add(SimpleConditionEvent.satisfied(item, "I'm always satisfied"));
        }
    };

    static final ArchCondition<JavaClass> NEVER_BE_SATISFIED = never(BE_SATISFIED)
            .as("satisfy something, but don't");

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
