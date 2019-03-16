package com.tngtech.archunit.junit;

import java.lang.reflect.Field;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.runners.model.InitializationError;

import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.testutil.ReflectionTestUtils.field;

class ArchUnitRunnerTestUtils {
    static final ArchCondition<JavaClass> BE_SATISFIED = new ArchCondition<JavaClass>("satisfy something") {
        @Override
        public void check(JavaClass item, ConditionEvents events) {
            events.add(SimpleConditionEvent.satisfied(item, "I'm always satisfied"));
        }
    };

    static final ArchCondition<JavaClass> NEVER_BE_SATISFIED = never(BE_SATISFIED)
            .as("satisfy something, but do not");

    static ArchUnitRunner newRunnerFor(Class<?> testClass) {
        try {
            return new ArchUnitRunner(testClass);
        } catch (InitializationError initializationError) {
            throw new RuntimeException(initializationError);
        }
    }

    static ArchUnitRunner newRunnerFor(Class<?> testClass, ArchUnitRunner.SharedCache cache) {
        try {
            ArchUnitRunner runner = newRunnerFor(testClass);
            Field field = field(ArchUnitRunner.class, "cache");
            field.setAccessible(true);
            field.set(runner, cache);
            return runner;
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
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
