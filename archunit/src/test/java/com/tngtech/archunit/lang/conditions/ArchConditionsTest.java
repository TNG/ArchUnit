package com.tngtech.archunit.lang.conditions;

import java.util.Collection;

import com.google.common.base.Joiner;
import com.tngtech.archunit.core.JavaCall;
import com.tngtech.archunit.core.JavaClass;
import com.tngtech.archunit.core.JavaMethod;
import com.tngtech.archunit.core.JavaMethodCall;
import com.tngtech.archunit.core.TestUtils.AccessesSimulator;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.FailureMessages;
import org.assertj.core.api.iterable.Extractor;
import org.junit.Test;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.collect.Sets.newTreeSet;
import static com.tngtech.archunit.core.TestUtils.javaClass;
import static com.tngtech.archunit.core.TestUtils.javaMethod;
import static com.tngtech.archunit.core.TestUtils.predicateWithDescription;
import static com.tngtech.archunit.core.TestUtils.simulateCall;
import static com.tngtech.archunit.lang.conditions.ArchConditions.accessClass;
import static com.tngtech.archunit.lang.conditions.ArchConditions.accessClassesThatResideIn;
import static com.tngtech.archunit.lang.conditions.ArchConditions.accessClassesThatResideInAnyPackage;
import static com.tngtech.archunit.lang.conditions.ArchConditions.accessField;
import static com.tngtech.archunit.lang.conditions.ArchConditions.accessFieldWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.callMethod;
import static com.tngtech.archunit.lang.conditions.ArchConditions.callMethodWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.containAnyElementThat;
import static com.tngtech.archunit.lang.conditions.ArchConditions.containOnlyElementsThat;
import static com.tngtech.archunit.lang.conditions.ArchConditions.getField;
import static com.tngtech.archunit.lang.conditions.ArchConditions.getFieldWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyBeAccessedByAnyPackage;
import static com.tngtech.archunit.lang.conditions.ArchConditions.resideInAPackage;
import static com.tngtech.archunit.lang.conditions.ArchConditions.setField;
import static com.tngtech.archunit.lang.conditions.ArchConditions.setFieldWhere;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.named;
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

        ConditionEvents events =
                check(never(callMethod("dontCallMe").inHierarchyOf(SomeSuperClass.class)), callingClass);

        assertThat(events).containViolations(callToDontCallMe.getDescription());

        events = new ConditionEvents();
        never(callMethod("dontCallMe").in(SomeSuperClass.class)).check(callingClass, events);
        assertThat(events).containNoViolation();
        assertThat(getOnlyElement(events.getAllowed())).extracting("allowed").extracting(TO_STRING_LEXICOGRAPHICALLY)
                .containsOnly(callToCallMe.getDescription() + callToDontCallMe.getDescription());
    }

    @Test
    public void access_class() {
        JavaClass clazz = javaClass(CallingClass.class);
        JavaCall<?> call = simulateCall().from(clazz, "call").to(SomeSuperClass.class, "callMe");

        ConditionEvents events = check(never(accessClass(named(".*Some.*"))), clazz);

        assertThat(events).containViolations(call.getDescription());

        events = check(never(accessClass(named(".*Wong.*"))), clazz);

        assertThat(events).containNoViolation();
    }

    @Test
    public void descriptions() {
        assertThat(accessClassesThatResideIn("..any..").getDescription())
                .isEqualTo("access classes that reside in '..any..'");

        assertThat(accessClassesThatResideInAnyPackage("..one..", "..two..").getDescription())
                .isEqualTo("access classes that reside in any package ['..one..', '..two..']");

        assertThat(onlyBeAccessedByAnyPackage("..one..", "..two..").getDescription())
                .isEqualTo("only be accessed by any package ['..one..', '..two..']");

        assertThat(getField(System.class, "out").getDescription())
                .isEqualTo("get field System.out");

        assertThat(getFieldWhere(predicateWithDescription("something")).getDescription())
                .isEqualTo("get field where something");

        assertThat(setField(System.class, "out").getDescription())
                .isEqualTo("set field System.out");

        assertThat(setFieldWhere(predicateWithDescription("something")).getDescription())
                .isEqualTo("set field where something");

        assertThat(accessField(System.class, "out").getDescription())
                .isEqualTo("access field System.out");

        assertThat(accessFieldWhere(predicateWithDescription("something")).getDescription())
                .isEqualTo("access field where something");

        assertThat(callMethodWhere(predicateWithDescription("something")).getDescription())
                .isEqualTo("call method where something");

        assertThat(accessClass(predicateWithDescription("something")).getDescription())
                .isEqualTo("access class something");

        assertThat(resideInAPackage("..any..").getDescription())
                .isEqualTo("reside in a package '..any..'");

        assertThat(never(conditionWithDescription("something")).getDescription())
                .isEqualTo("never something");

        assertThat(containAnyElementThat(conditionWithDescription("something")).getDescription())
                .isEqualTo("contain any element that something");

        assertThat(containOnlyElementsThat(conditionWithDescription("something")).getDescription())
                .isEqualTo("contain only elements that something");
    }

    private ArchCondition<Object> conditionWithDescription(String description) {
        return new ArchCondition<Object>(description) {
            @Override
            public void check(Object item, ConditionEvents events) {
            }
        };
    }

    private ConditionEvents check(ArchCondition<JavaClass> condition, JavaClass javaClass) {
        ConditionEvents events = new ConditionEvents();
        condition.check(javaClass, events);
        return events;
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