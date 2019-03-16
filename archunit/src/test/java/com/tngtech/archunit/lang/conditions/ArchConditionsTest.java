package com.tngtech.archunit.lang.conditions;

import java.sql.SQLException;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaCall;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.TestUtils.AccessesSimulator;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import org.junit.Test;

import static com.tngtech.archunit.core.domain.JavaCall.Predicates.target;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.core.domain.TestUtils.importClassWithContext;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.core.domain.TestUtils.importMethod;
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
import static com.tngtech.archunit.lang.conditions.ArchConditions.declareThrowableOfType;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyBeAccessedByAnyPackage;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyHaveDependentsInAnyPackage;
import static com.tngtech.archunit.lang.conditions.ArchConditions.onlyHaveDependentsWhere;
import static com.tngtech.archunit.testutil.Assertions.assertThat;
import static java.util.regex.Pattern.quote;

public class ArchConditionsTest {
    @Test
    public void never_call_method_where_target_owner_is_assignable_to() {
        JavaClass callingClass = importClassWithContext(CallingClass.class);
        AccessesSimulator simulateCall = simulateCall();
        JavaClass someClass = importClassWithContext(SomeClass.class);
        JavaMethod doNotCallMe = someClass.getMethod("doNotCallMe");
        JavaMethodCall callTodoNotCallMe = simulateCall.from(callingClass.getMethod("call"), 0).to(doNotCallMe);

        ArchCondition<JavaClass> condition = never(callMethodWhere(target(name("doNotCallMe"))
                .and(target(owner(assignableTo(SomeSuperClass.class))))));
        assertThat(condition).checking(callingClass)
                .containViolations(callTodoNotCallMe.getDescription());

        condition = never(callMethodWhere(target(name("doNotCallMe")).and(target(owner(type(SomeSuperClass.class))))));
        assertThat(condition).checking(callingClass)
                .containNoViolation();
    }

    @Test
    public void access_class() {
        JavaClass clazz = importClassWithContext(CallingClass.class);
        JavaCall<?> call = simulateCall().from(clazz, "call").to(SomeClass.class, "callMe");

        assertThat(never(accessClassesThat(nameMatching(".*Some.*")))).checking(clazz)
                .containViolations(call.getDescription());

        assertThat(never(accessClassesThat(nameMatching(".*Wrong.*")))).checking(clazz)
                .containNoViolation();
    }

    @Test
    public void descriptions() {
        assertThat(accessClassesThatResideIn("..any.."))
                .hasDescription("access classes that reside in package '..any..'");

        assertThat(accessClassesThatResideInAnyPackage("..one..", "..two.."))
                .hasDescription("access classes that reside in any package ['..one..', '..two..']");

        assertThat(onlyBeAccessedByAnyPackage("..one..", "..two.."))
                .hasDescription("only be accessed by any package ['..one..', '..two..']");

        assertThat(onlyHaveDependentsInAnyPackage("..one..", "..two.."))
                .hasDescription("only have dependents in any package ['..one..', '..two..']");

        assertThat(callCodeUnitWhere(predicateWithDescription("something")))
                .hasDescription("call code unit where something");

        assertThat(accessClassesThat(predicateWithDescription("something")))
                .hasDescription("access classes that something");

        assertThat(never(conditionWithDescription("something")))
                .hasDescription("never something");

        assertThat(containAnyElementThat(conditionWithDescription("something")))
                .hasDescription("contain any element that something");

        assertThat(containOnlyElementsThat(conditionWithDescription("something")))
                .hasDescription("contain only elements that something");
    }

    @Test
    public void only_have_dependents_where() {
        JavaClasses classes = importClasses(CallingClass.class, SomeClass.class);
        JavaClass accessedClass = classes.get(SomeClass.class);

        assertThat(onlyHaveDependentsWhere(DescribedPredicate.<Dependency>alwaysFalse()))
                .checking(accessedClass)
                .haveAtLeastOneViolationMessageMatching(String.format(".*%s.*%s.*",
                        quote(CallingClass.class.getName()), quote(SomeClass.class.getName())));

        assertThat(onlyHaveDependentsWhere(DescribedPredicate.<Dependency>alwaysTrue().as("custom")))
                .hasDescription("only have dependents where custom")
                .checking(accessedClass)
                .containNoViolation();
    }

    @Test
    public void declare_throwable_of_type() {
        class Failure {
            @SuppressWarnings("unused")
            void method() {
            }
        }
        assertThat(declareThrowableOfType(SQLException.class))
                .hasDescription("declare throwable of type " + SQLException.class.getName())
                .checking(importMethod(Failure.class, "method"))
                .haveOneViolationMessageContaining("Method", "method()", "does not declare throwable of type " + SQLException.class.getName());
    }

    private ArchCondition<Object> conditionWithDescription(String description) {
        return new ArchCondition<Object>(description) {
            @Override
            public void check(Object item, ConditionEvents events) {
            }
        };
    }

    private static class CallingClass {
        void call() {
            new SomeClass().doNotCallMe();
            new SomeClass().callMe();
        }
    }

    private static class SomeClass extends SomeSuperClass {
        void doNotCallMe() {
        }

        void callMe() {
        }
    }

    private static class SomeSuperClass {
    }
}