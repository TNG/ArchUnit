package com.tngtech.archunit.lang.conditions;

import java.util.Collection;
import java.util.List;

import com.google.common.collect.Lists;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import org.assertj.core.api.Condition;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.lang.conditions.ArchPredicates.targetIs;
import static com.tngtech.archunit.lang.conditions.testobjects.TestObjects.CALLER_CLASS;
import static com.tngtech.archunit.lang.conditions.testobjects.TestObjects.TARGET_CLASS;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Theories.class)
public class MethodCallConditionTest {

    @DataPoint
    public static final MethodCallToAnalyse properMethodCall = properMethodCallToTargetFrom(CALLER_CLASS);

    @DataPoint
    public static final MethodCallToAnalyse constructorCall = constructorCallToTargetFrom(CALLER_CLASS);

    @Theory
    public void condition_is_satisfied_by_matching_call(MethodCallToAnalyse callToAnalyse) {
        MethodCallCondition targetMethodCallCondition = callConditionBuilderMatching(callToAnalyse).build();
        ConditionEvents events = new ConditionEvents();
        targetMethodCallCondition.check(callToAnalyse.call, events);
        boolean satisfied = !events.containViolation();

        assertThat(satisfied).as("Condition is satisfied").isTrue();
        assertThat(events.getViolating()).isEmpty();
        assertThat(events.getAllowed()).is(containingMessageFor(callToAnalyse));
    }

    @Theory
    public void condition_is_not_satisfied_on_target_mismatch(MethodCallToAnalyse callToAnalyse) {
        MethodCallCondition targetMethodCallCondition = callConditionBuilderMatching(callToAnalyse)
                .withTarget(getClass())
                .build();
        ConditionEvents events = new ConditionEvents();
        targetMethodCallCondition.check(callToAnalyse.call, events);
        boolean satisfied = !events.containViolation();

        assertThat(satisfied).as("Condition is satisfied").isFalse();
        assertThat(events.getViolating()).is(containingMessageFor(callToAnalyse));
        assertThat(events.getAllowed()).isEmpty();
    }

    @Theory
    public void condition_is_not_satisfied_on_name_mismatch(MethodCallToAnalyse callToAnalyse) {
        MethodCallCondition targetMethodCallCondition = callConditionBuilderMatching(callToAnalyse)
                .withName("wrong")
                .build();
        ConditionEvents events = new ConditionEvents();
        targetMethodCallCondition.check(callToAnalyse.call, events);
        boolean satisfied = !events.containViolation();

        assertThat(satisfied).as("Condition is satisfied").isFalse();
        assertThat(events.getViolating()).is(containingMessageFor(callToAnalyse));
        assertThat(events.getAllowed()).isEmpty();
    }

    @Theory
    public void condition_is_not_satisfied_on_parameter_type_mismatch(MethodCallToAnalyse callToAnalyse) {
        MethodCallCondition targetMethodCallCondition = callConditionBuilderMatching(callToAnalyse)
                .withParameters(getClass())
                .build();
        ConditionEvents events = new ConditionEvents();
        targetMethodCallCondition.check(callToAnalyse.call, events);
        boolean satisfied = !events.containViolation();

        assertThat(satisfied).as("Condition is satisfied").isFalse();
        assertThat(events.getViolating()).is(containingMessageFor(callToAnalyse));
        assertThat(events.getAllowed()).isEmpty();
    }

    private MethodCallConditionBuilder callConditionBuilderMatching(MethodCallToAnalyse callToAnalyse) {
        return new MethodCallConditionBuilder(callToAnalyse);
    }

    private static MethodCallToAnalyse properMethodCallToTargetFrom(JavaClass callerClass) {
        return new MethodCallToAnalyse(callerClass.getProperMethodCalls());
    }

    private static MethodCallToAnalyse constructorCallToTargetFrom(JavaClass callerClass) {
        return new MethodCallToAnalyse(callerClass.getConstructorCalls());
    }

    private static Condition<Iterable<? extends ConditionEvent>> containingMessageFor(final MethodCallToAnalyse callToAnalyse) {
        final String originName = callToAnalyse.call.getOrigin().getFullName();
        final String targetName = callToAnalyse.call.getTarget().getFullName();
        final String ideJumpHook = ideJumpHookFor(callToAnalyse);

        return new Condition<Iterable<? extends ConditionEvent>>() {
            @Override
            public boolean matches(Iterable<? extends ConditionEvent> value) {
                boolean matches = false;
                for (ConditionEvent event : value) {
                    matches = matches || eventHasCorrectDetails(event);
                }
                return matches;
            }

            private boolean eventHasCorrectDetails(ConditionEvent event) {
                return event.toString().contains(originName)
                        && event.toString().contains(targetName)
                        && event.toString().contains(ideJumpHook);
            }
        }.as(String.format("Event from call with origin %s, target %s and IDE Hook %s", originName, targetName, ideJumpHook));
    }

    private static String ideJumpHookFor(MethodCallToAnalyse callToAnalyse) {
        String simpleCallerName = callToAnalyse.call.getOriginClass().getSimpleName();
        int lineNumber = callToAnalyse.call.getLineNumber();
        return String.format("(%s.java:%d)", simpleCallerName, lineNumber);
    }

    private static class MethodCallToAnalyse {
        private final JavaCall<?> call;

        private MethodCallToAnalyse(Collection<? extends JavaCall<?>> calls) {
            call = callToTargetIn(calls);
        }

        private JavaCall<?> callToTargetIn(Collection<? extends JavaCall<?>> calls) {
            for (JavaCall<?> call : calls) {
                if (call.getTarget().getOwner().equals(TARGET_CLASS)) {
                    return call;
                }
            }
            throw new RuntimeException("Couldn't find any matching call to " + TARGET_CLASS + " in " + calls);
        }
    }

    private static class MethodCallConditionBuilder {
        private Class<?> targetClass;
        private String methodName;
        private List<Class<?>> paramTypes;

        private MethodCallConditionBuilder(MethodCallToAnalyse callToAnalyse) {
            targetClass = callToAnalyse.call.getTarget().getOwner().reflect();
            methodName = callToAnalyse.call.getTarget().getName();
            paramTypes = callToAnalyse.call.getTarget().getParameters();
        }

        private MethodCallConditionBuilder withTarget(Class<?> targetClass) {
            this.targetClass = targetClass;
            return this;
        }

        public MethodCallConditionBuilder withName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public MethodCallConditionBuilder withParameters(Class<?> paramTypes) {
            this.paramTypes = Lists.<Class<?>>newArrayList(paramTypes);
            return this;
        }

        private MethodCallCondition build() {
            return new MethodCallCondition(targetIs(targetClass, methodName, paramTypes));
        }
    }
}