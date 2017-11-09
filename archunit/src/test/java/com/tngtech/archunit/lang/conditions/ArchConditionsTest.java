package com.tngtech.archunit.lang.conditions;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.common.base.Joiner;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.TestUtils.AccessesSimulator;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.CollectsLines;
import com.tngtech.archunit.lang.ConditionEvent;
import com.tngtech.archunit.lang.ConditionEvents;
import org.assertj.core.api.iterable.Extractor;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.core.domain.TestUtils.predicateWithDescription;
import static com.tngtech.archunit.core.domain.TestUtils.simulateCall;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;
import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.nameMatching;
import static com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner;
import static com.tngtech.archunit.lang.conditions.ArchConditions.accessClassesThat;
import static com.tngtech.archunit.lang.conditions.ArchConditions.accessClassesThatResideIn;
import static com.tngtech.archunit.lang.conditions.ArchConditions.accessClassesThatResideInAnyPackage;
import static com.tngtech.archunit.lang.conditions.ArchConditions.callCodeUnitWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.callMethodWhere;
import static com.tngtech.archunit.lang.conditions.ArchConditions.containAnyElementThat;
import static com.tngtech.archunit.lang.conditions.ArchConditions.containOnlyElementsThat;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyBeAccessedByAnyPackage;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyHaveDependentsInAnyPackage;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyHaveDependentsWhere;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static java.util.regex.Pattern.quote;

public class ArchConditionsTest {
    @Test
    public void never_call_method_where_target_owner_is_assignable_to() throws NoSuchMethodException {
        JavaClass callingClass = importClassWithContext(CallingClass.class);
        AccessesSimulator simulateCall = simulateCall();
        JavaClass someClass = importClassWithContext(SomeClass.class);
        JavaMethod dontCallMe = someClass.getMethod("dontCallMe");
        JavaMethodCall callToDontCallMe = simulateCall.from(callingClass.getMethod("call"), 0).to(dontCallMe);

        ConditionEvents events =
                check(never(callMethodWhere(target(name("dontCallMe"))
                        .and(target(owner(assignableTo(SomeSuperClass.class)))))), callingClass);

        assertThat(events).containViolations(callToDontCallMe.getDescription());

        events = new ConditionEvents();
        never(callMethodWhere(target(name("dontCallMe")).and(target(owner(type(SomeSuperClass.class))))))
                .check(callingClass, events);
        assertThat(events).containNoViolation();
    }

    @Test
    public void access_class() {
        JavaClass clazz = importClassWithContext(CallingClass.class);
        JavaCall<?> call = simulateCall().from(clazz, "call").to(SomeClass.class, "callMe");

        ConditionEvents events = check(never(accessClassesThat(nameMatching(".*Some.*"))), clazz);

        assertThat(events).containViolations(call.getDescription());

        events = check(never(accessClassesThat(nameMatching(".*Wrong.*"))), clazz);

        assertThat(events).containNoViolation();
    }

    @Test
    public void descriptions() {
        assertThat(accessClassesThatResideIn("..any..").getDescription())
                .isEqualTo("access classes that reside in package '..any..'");

        assertThat(accessClassesThatResideInAnyPackage("..one..", "..two..").getDescription())
                .isEqualTo("access classes that reside in any package ['..one..', '..two..']");

        assertThat(onlyBeAccessedByAnyPackage("..one..", "..two..").getDescription())
                .isEqualTo("only be accessed by any package ['..one..', '..two..']");

        assertThat(onlyHaveDependentsInAnyPackage("..one..", "..two..").getDescription())
                .isEqualTo("only have dependents in any package ['..one..', '..two..']");

        assertThat(callCodeUnitWhere(predicateWithDescription("something")).getDescription())
                .isEqualTo("call code unit where something");

        assertThat(accessClassesThat(predicateWithDescription("something")).getDescription())
                .isEqualTo("access classes that something");

        assertThat(never(conditionWithDescription("something")).getDescription())
                .isEqualTo("never something");

        assertThat(containAnyElementThat(conditionWithDescription("something")).getDescription())
                .isEqualTo("contain any element that something");

        assertThat(containOnlyElementsThat(conditionWithDescription("something")).getDescription())
                .isEqualTo("contain only elements that something");
    }

    @Test
    public void only_have_dependents_where() {
        JavaClasses classes = importClasses(CallingClass.class, SomeClass.class);
        JavaClass accessedClass = classes.get(SomeClass.class);

        ConditionEvents events = check(onlyHaveDependentsWhere(DescribedPredicate.<Dependency>alwaysFalse()), accessedClass);
        assertThat(events).haveAtLeastOneViolationMessageMatching(String.format(".*%s.*%s.*",
                quote(CallingClass.class.getName()), quote(SomeClass.class.getName())));

        events = check(onlyHaveDependentsWhere(DescribedPredicate.<Dependency>alwaysTrue()), accessedClass);
        assertThat(events).containNoViolation();

        DescribedPredicate<Dependency> customDescription = DescribedPredicate.<Dependency>alwaysTrue().as("custom");
        assertThat(onlyHaveDependentsWhere(customDescription).getDescription()).isEqualTo("only have dependents where custom");
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
            final SortedSet<String> lines = new TreeSet<>();
            CollectsLines messages = new CollectsLines() {
                @Override
                public void add(String message) {
                    lines.add(message);
                }
            };
            for (ConditionEvent event : ((Collection<ConditionEvent>) input)) {
                event.describeTo(messages);
            }
            return Joiner.on("").join(lines);
        }
    };

    private static class CallingClass {
        void call() {
            new SomeClass().dontCallMe();
            new SomeClass().callMe();
        }
    }

    private static class SomeClass extends SomeSuperClass {
        void dontCallMe() {
        }

        void callMe() {
        }
    }

    private static class SomeSuperClass {
    }
}