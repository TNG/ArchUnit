package com.tngtech.archunit.lang.conditions;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.conditions.testobjects.CallerClass;
import com.tngtech.archunit.lang.conditions.testobjects.TargetClass;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.core.domain.properties.HasParameterTypes.Predicates.rawParameterTypes;
import static com.tngtech.archunit.lang.conditions.ArchConditions.callMethodWhere;
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
                callMethodWhere(target(name(TargetClass.appendStringMethod))
                        .and(target(rawParameterTypes(TargetClass.appendStringParams)))
                        .and(target(owner(type(TargetClass.class))))));

        assertThat(events).containNoViolation();
    }

    @Test
    public void call_without_argument_doesnt_match() {
        ConditionEvents events = checkCondition(callMethodWhere(
                target(rawParameterTypes(new Class[0]))
                        .and(target(name(TargetClass.appendStringMethod))
                                .and(target(owner(type(TargetClass.class)))))));

        assertThat(events).haveOneViolationMessageContaining(VIOLATION_MESSAGE_PARTS);
    }

    @Test
    public void call_with_wrong_method_name_doesnt_match() {
        ConditionEvents events = checkCondition(
                callMethodWhere(target(name("wrong"))
                        .and(target(rawParameterTypes(TargetClass.appendStringParams)))
                        .and(target(owner(type(TargetClass.class))))));

        assertThat(events).haveOneViolationMessageContaining(VIOLATION_MESSAGE_PARTS);
    }

    private ConditionEvents checkCondition(ArchCondition<JavaClass> condition) {
        ConditionEvents events = new ConditionEvents();
        condition.check(CALLER_CLASS, events);
        return events;
    }
}