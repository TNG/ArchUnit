package com.tngtech.archunit.lang.conditions;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.conditions.testobjects.CallerClass;
import com.tngtech.archunit.lang.conditions.testobjects.TargetClass;
import org.junit.Test;

import static com.tngtech.archunit.lang.conditions.ArchConditions.callMethod;
import static com.tngtech.archunit.lang.conditions.testobjects.TestObjects.CALLER_CLASS;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class ClassCallsCodeUnitConditionTest {
    private static final Set<String> VIOLATION_MESSAGE_PARTS = ImmutableSet.of(
            CallerClass.methodThatCallsAppendString,
            TargetClass.appendStringMethod,
            CallerClass.callOfAppendStringLineNumber);

    @Test
    public void call_with_correct_name_and_params_matches() {
        ConditionEvents events = checkCondition(
                callMethod(TargetClass.appendStringMethod, TargetClass.appendStringParams).in(TargetClass.class));

        assertThat(events).containNoViolation();
    }

    @Test
    public void call_without_argument_doesnt_match() {
        ConditionEvents events = checkCondition(callMethod(TargetClass.appendStringMethod).in(TargetClass.class));

        assertThat(events).haveOneViolationMessageContaining(VIOLATION_MESSAGE_PARTS);
    }

    @Test
    public void call_with_wrong_method_name_doesnt_match() {
        ConditionEvents events = checkCondition(callMethod("wrong", TargetClass.appendStringParams).in(TargetClass.class));

        assertThat(events).haveOneViolationMessageContaining(VIOLATION_MESSAGE_PARTS);
    }

    private ConditionEvents checkCondition(ArchCondition<JavaClass> condition) {
        ConditionEvents events = new ConditionEvents();
        condition.check(CALLER_CLASS, events);
        return events;
    }
}