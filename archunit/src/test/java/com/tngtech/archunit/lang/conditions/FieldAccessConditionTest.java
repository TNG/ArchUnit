package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.google.common.base.Joiner;
import com.tngtech.archunit.core.domain.JavaFieldAccess;
import com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.conditions.FieldAccessCondition.FieldGetAccessCondition;
import com.tngtech.archunit.lang.conditions.FieldAccessCondition.FieldSetAccessCondition;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType.GET;
import static com.tngtech.archunit.core.domain.JavaFieldAccess.AccessType.SET;
import static com.tngtech.archunit.core.domain.JavaFieldAccess.Predicates.target;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.lang.conditions.testobjects.TestObjects.CALLER_CLASS;
import static com.tngtech.archunit.lang.conditions.testobjects.TestObjects.TARGET_CLASS;
import static org.assertj.core.api.Assertions.assertThat;

public class FieldAccessConditionTest {
    @Test
    public void FieldGetAccessCondition_only_satisfied_on_get_field() {
        JavaFieldAccess getAccess = accessFromCallerToTargetWithType(GET);
        FieldGetAccessCondition getFieldCondition = new FieldGetAccessCondition(
                target(name(getAccess.getTarget().getName())));
        assertSatisfied(getFieldCondition, getAccess);

        JavaFieldAccess setAccess = accessFromCallerToTargetWithType(SET);
        getFieldCondition = new FieldGetAccessCondition(
                target(name(setAccess.getTarget().getName())));
        assertViolatedWithMessage(getFieldCondition, setAccess, "sets");
    }

    @Test
    public void FieldSetAccessCondition_only_satisfied_on_set_field() {
        JavaFieldAccess setAccess = accessFromCallerToTargetWithType(SET);
        FieldSetAccessCondition setFieldCondition = new FieldSetAccessCondition(
                target(name(setAccess.getTarget().getName())));
        assertSatisfied(setFieldCondition, setAccess);

        JavaFieldAccess getAccess = accessFromCallerToTargetWithType(GET);
        setFieldCondition = new FieldSetAccessCondition(
                target(name(getAccess.getTarget().getName())));
        assertViolatedWithMessage(setFieldCondition, getAccess, "gets");
    }

    @Test
    public void FieldAccessCondition_satisfied_on_both_get_and_set_field() {
        JavaFieldAccess setAccess = accessFromCallerToTargetWithType(SET);
        FieldAccessCondition setFieldCondition = new FieldAccessCondition(
                target(name(setAccess.getTarget().getName())));
        assertSatisfied(setFieldCondition, setAccess);

        JavaFieldAccess getAccess = accessFromCallerToTargetWithType(GET);
        setFieldCondition = new FieldAccessCondition(
                target(name(getAccess.getTarget().getName())));
        assertSatisfied(setFieldCondition, getAccess);
    }

    private void assertSatisfied(FieldAccessCondition getFieldCondition, JavaFieldAccess access) {
        ConditionEvents events = checkCondition(getFieldCondition, access);
        assertThat(events.containViolation()).as("Events contain violation").isFalse();
        assertThat(events.getViolating()).isEmpty();
    }

    private void assertViolatedWithMessage(
            FieldAccessCondition getFieldCondition,
            JavaFieldAccess access,
            String accessDescription
    ) {
        ConditionEvents events = checkCondition(getFieldCondition, access);

        assertThat(events.containViolation()).as("Events contain violation").isTrue();
        assertDescription(access, accessDescription, messageOf(events.getViolating()));
    }

    private ConditionEvents checkCondition(FieldAccessCondition getFieldCondition, JavaFieldAccess access) {
        ConditionEvents events = ConditionEvents.Factory.create();
        getFieldCondition.check(access, events);
        return events;
    }

    private String messageOf(Collection<ConditionEvent> events) {
        return Joiner.on(" ").join(events);
    }

    private void assertDescription(JavaFieldAccess access, String accessText, String description) {
        assertThat(description)
                .contains(accessText)
                .contains(access.getOrigin().getFullName())
                .contains(access.getTarget().getFullName())
                .contains("" + access.getLineNumber());
    }

    private static JavaFieldAccess accessFromCallerToTargetWithType(AccessType type) {
        for (JavaFieldAccess access : CALLER_CLASS.getFieldAccessesFromSelf()) {
            if (access.getTarget().getOwner().equals(TARGET_CLASS) && access.getAccessType() == type) {
                return access;
            }
        }
        throw new RuntimeException(CALLER_CLASS + " has no field access with type " + type);
    }
}
