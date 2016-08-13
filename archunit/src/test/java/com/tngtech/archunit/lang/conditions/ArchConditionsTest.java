package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.google.common.base.Joiner;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaMethod;
import com.tngtech.archunit.core.JavaMethodCall;
import com.tngtech.archunit.core.TestUtils.AccessesSimulator;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.FailureMessages;
import org.assertj.core.api.iterable.Extractor;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Sets.newTreeSet;
import static com.tngtech.archunit.core.TestUtils.javaClass;
import static com.tngtech.archunit.core.TestUtils.javaMethod;
import static com.tngtech.archunit.core.TestUtils.simulateCall;
import static com.tngtech.archunit.lang.conditions.ArchConditions.callMethod;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.testutil.Assertions.assertThat;

public class ArchConditionsTest {
    @Test
    public void never_call_method_in_hierarchy_of() throws NoSuchMethodException {
        JavaClass callingClass = javaClass(CallingClass.class);
        AccessesSimulator simulateCall = simulateCall();
        JavaMethod dontCallMe = javaMethod(javaClass(SomeClass.class), SomeSuperClass.class.getDeclaredMethod("dontCallMe"));
        JavaMethodCall callToDontCallMe = simulateCall.from(callingClass.getMethod("call"), 0).to(dontCallMe);
        JavaMethod callMe = javaMethod(javaClass(SomeClass.class), SomeSuperClass.class.getDeclaredMethod("callMe"));
        JavaMethodCall callToCallMe = simulateCall.from(callingClass.getMethod("call"), 0).to(callMe);

        ConditionEvents events = new ConditionEvents();
        never(callMethod("dontCallMe").inHierarchyOf(SomeSuperClass.class)).check(callingClass, events);
        assertThat(events).containViolations(callToDontCallMe.getDescription());

        events = new ConditionEvents();
        never(callMethod("dontCallMe").in(SomeSuperClass.class)).check(callingClass, events);
        assertThat(events).containNoViolation();
        assertThat(getOnlyElement(events.getAllowed())).extracting("allowed").extracting(TO_STRING_LEXICOGRAPHICALLY)
                .containsOnly(callToCallMe.getDescription() + callToDontCallMe.getDescription());
    }

    private static final Extractor<Object, String> TO_STRING_LEXICOGRAPHICALLY = new Extractor<Object, String>() {
        @SuppressWarnings("unchecked")
        @Override
        public String extract(Object input) {
            FailureMessages messages = new FailureMessages();
            for (ConditionEvent event : ((Collection<ConditionEvent>) input)) {
                event.describeTo(messages);
            }
            return Joiner.on("").join(newTreeSet(messages));
        }
    };

    private static class CallingClass {
        void call() {
            new SomeClass().dontCallMe();
            new SomeClass().callMe();
        }
    }

    private static class SomeClass extends SomeSuperClass {
    }

    private static class SomeSuperClass {
        void dontCallMe() {
        }

        void callMe() {
        }
    }
}