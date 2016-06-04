package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.google.common.base.Joiner;
import com.tngtech.archunit.core.JavaFieldAccess;
import com.tngtech.archunit.core.JavaFieldAccess.AccessType;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.conditions.FieldAccessCondition.FieldGetAccessCondition;
import com.tngtech.archunit.lang.conditions.FieldAccessCondition.FieldSetAccessCondition;
import com.tngtech.archunit.lang.conditions.testobjects.TargetClass;
import org.junit.Test;

import static com.tngtech.archunit.core.JavaFieldAccess.AccessType.GET;
import static com.tngtech.archunit.core.JavaFieldAccess.AccessType.SET;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.ownerAndName;
import static com.tngtech.archunit.lang.conditions.testobjects.TestObjects.CALLER_CLASS;
import static com.tngtech.archunit.lang.conditions.testobjects.TestObjects.TARGET_CLASS;
import static org.assertj.core.api.Assertions.assertThat;

public class FieldAccessConditionTest {
    @Test
    public void condition_satisfied_on_get_field() {
        JavaFieldAccess getAccess = accessFromCallerToTargetWithType(GET);

        FieldAccessCondition getFieldCondition = new FieldGetAccessCondition(
                ownerAndName(TargetClass.class, getAccess.getTarget().getName()));
        ConditionEvents events = new ConditionEvents();
        getFieldCondition.check(getAccess, events);
        boolean satisfied = !events.containViolation();

        assertThat(satisfied).isTrue();
        assertReadMessage(getAccess, messageOf(events.getAllowed()));
        assertThat(events.getViolating()).isEmpty();
    }

    @Test
    public void condition_on_get_field_not_satisfied_when_set_field() {
        JavaFieldAccess setAccess = accessFromCallerToTargetWithType(SET);

        FieldAccessCondition getFieldCondition = new FieldGetAccessCondition(
                ownerAndName(TargetClass.class, setAccess.getTarget().getName()));
        ConditionEvents events = new ConditionEvents();
        getFieldCondition.check(setAccess, events);
        boolean satisfied = !events.containViolation();

        assertThat(satisfied).isFalse();
        assertThat(events.getAllowed()).isEmpty();
        assertReadMessage(setAccess, messageOf(events.getViolating()));
    }

    @Test
    public void condition_satisfied_on_set_field() {
        JavaFieldAccess setAccess = accessFromCallerToTargetWithType(SET);

        FieldAccessCondition setFieldCondition = new FieldSetAccessCondition(
                ownerAndName(TargetClass.class, setAccess.getTarget().getName()));
        ConditionEvents events = new ConditionEvents();
        setFieldCondition.check(setAccess, events);
        boolean satisfied = !events.containViolation();

        assertThat(satisfied).isTrue();
        assertWriteMessage(setAccess, messageOf(events.getAllowed()));
        assertThat(events.getViolating()).isEmpty();
    }

    @Test
    public void condition_satisfied_on_access_field() {
        JavaFieldAccess getAccess = accessFromCallerToTargetWithType(GET);

        FieldAccessCondition getFieldCondition = new FieldAccessCondition(
                ownerAndName(TargetClass.class, getAccess.getTarget().getName()));
        ConditionEvents events = new ConditionEvents();
        getFieldCondition.check(getAccess, events);
        boolean satisfied = !events.containViolation();

        assertThat(satisfied).isTrue();
        assertAccessMessage(getAccess, messageOf(events.getAllowed()));
        assertThat(events.getViolating()).isEmpty();
    }

    private String messageOf(Collection<ConditionEvent> events) {
        return Joiner.on(" ").join(events);
    }

    private void assertAccessMessage(JavaFieldAccess getAccess, String description) {
        assertDescription(getAccess, "accesses", description);
    }

    private void assertReadMessage(JavaFieldAccess getAccess, String description) {
        assertDescription(getAccess, "gets", description);
    }

    private void assertWriteMessage(JavaFieldAccess setAccess, String description) {
        assertDescription(setAccess, "sets", description);
    }

    private void assertDescription(JavaFieldAccess access, String accessText, String description) {
        assertThat(description)
                .contains(accessText)
                .contains(access.getOrigin().getFullName())
                .contains(access.getTarget().getFullName())
                .contains("" + access.getLineNumber());
    }

    static JavaFieldAccess accessFromCallerToTargetWithType(AccessType type) {
        for (JavaFieldAccess access : CALLER_CLASS.getFieldAccessesFromSelf()) {
            if (access.getTarget().getOwner().equals(TARGET_CLASS) && access.getAccessType() == type) {
                return access;
            }
        }
        throw new RuntimeException(CALLER_CLASS + " has no field access with type " + type);
    }
}