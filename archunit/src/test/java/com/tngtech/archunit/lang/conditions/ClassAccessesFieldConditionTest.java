package com.tngtech.archunit.lang.conditions;

import java.util.Set;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.conditions.testobjects.CallerClass;
import com.tngtech.archunit.lang.conditions.testobjects.TargetClass;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static com.google.common.collect.Sets.newHashSet;
import static com.tngtech.archunit.lang.conditions.ArchConditions.accessField;
import static com.tngtech.archunit.lang.conditions.ArchConditions.getField;
import static com.tngtech.archunit.lang.conditions.ArchConditions.setField;
import static com.tngtech.archunit.lang.conditions.testobjects.TestObjects.CALLER_CLASS;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

@RunWith(Theories.class)
public class ClassAccessesFieldConditionTest {
    public static final AccessInfo getAccess = new AccessInfo(
            CallerClass.methodThatGetsPublicString, TargetClass.publicStringField, CallerClass.getAccessOfPublicStringLineNumbers);
    public static final AccessInfo setAccess = new AccessInfo(
            CallerClass.methodThatSetsPublicString, TargetClass.publicStringField, CallerClass.setAccessOfPublicStringLineNumbers);

    @DataPoint
    public static final PositiveTestCase existingGetAccess = positiveTestCase()
            .accessInfo(getAccess)
            .condition(getField(TargetClass.class, getAccess.targetName));

    @DataPoint
    public static final NegativeTestCase nonExistingGetAccess = negativeTestCase()
            .accessInfo(getAccess)
            .condition(getField(TargetClass.class, "nonExisting"));

    @DataPoint
    public static final PositiveTestCase existingSetAccess = positiveTestCase()
            .accessInfo(setAccess)
            .condition(setField(TargetClass.class, setAccess.targetName));

    @DataPoint
    public static final NegativeTestCase nonExistingSetAccess = negativeTestCase()
            .accessInfo(setAccess)
            .condition(setField(TargetClass.class, "nonExisting"));

    @DataPoint
    public static final PositiveTestCase existingArbitraryAccess = positiveTestCase()
            .accessInfo(getAccess)
            .condition(accessField(TargetClass.class, TargetClass.publicStringField));

    @DataPoint
    public static final NegativeTestCase nonExistingArbitraryAccess = negativeTestCase()
            .accessInfo(getAccess)
            .condition(accessField(TargetClass.class, "nonExisting"));

    @Theory
    public void condition_works(PositiveTestCase testCase) {
        ConditionEvents events = new ConditionEvents();

        testCase.condition.check(CALLER_CLASS, events);

        assertThat(events).containNoViolation();
    }

    @Theory
    public void condition_works(NegativeTestCase testCase) {
        ConditionEvents events = new ConditionEvents();

        testCase.condition.check(CALLER_CLASS, events);

        assertThat(events).haveOneViolationMessageContaining(testCase.violationMessageParts());
    }

    private static class AccessInfo {
        private final String callerName;
        private final String targetName;
        private final String[] lineNumbers;

        private AccessInfo(String callerName, String targetName, String[] lineNumbers) {
            this.callerName = callerName;
            this.targetName = targetName;
            this.lineNumbers = lineNumbers;
        }
    }

    static PositiveTestCase positiveTestCase() {
        return new PositiveTestCase();
    }

    static NegativeTestCase negativeTestCase() {
        return new NegativeTestCase();
    }

    static class PositiveTestCase extends TestCase<PositiveTestCase> {
    }

    static class NegativeTestCase extends TestCase<NegativeTestCase> {
    }

    abstract static class TestCase<SELF extends TestCase<SELF>> {
        AccessInfo accessInfo;
        ArchCondition<JavaClass> condition;

        Set<String> violationMessageParts() {
            Set<String> parts = newHashSet(accessInfo.callerName, accessInfo.targetName);
            parts.addAll(newHashSet(accessInfo.lineNumbers));
            return parts;
        }

        SELF accessInfo(final AccessInfo accessInfo) {
            this.accessInfo = accessInfo;
            return self();
        }

        SELF condition(ArchCondition<JavaClass> condition) {
            this.condition = condition;
            return self();
        }

        @SuppressWarnings("unchecked")
        private SELF self() {
            return (SELF) this;
        }
    }
}
