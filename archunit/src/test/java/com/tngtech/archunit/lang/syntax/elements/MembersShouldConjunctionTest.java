package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.FailureReport;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.conditions.ArchConditions.never;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.members;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class MembersShouldConjunctionTest {
    @DataProvider
    public static Object[][] ORed_conditions() {
        return testForEach(members()
                .should(beDeclaredIn(RightOne.class))
                .orShould(beDeclaredIn(RightTwo.class)));
        // FIXME: Add cases for fluent versions
    }

    @Test
    @UseDataProvider("ORed_conditions")
    public void orShould_ORs_conditions(ArchRule rule) {
        EvaluationResult result = rule
                .evaluate(importClasses(RightOne.class, RightTwo.class, WrongOne.class));

        FailureReport report = result.getFailureReport();
        assertThat(report.toString())
                .contains(String.format(
                        "members should be declared in %s or should be declared in %s",
                        RightOne.class.getName(), RightTwo.class.getName()));
        assertThat(report.getDetails()).containsOnly(
                String.format("%s and %s",
                        isNotDeclaredInMessage(WrongOne.class, CONSTRUCTOR_NAME, RightOne.class),
                        isNotDeclaredInMessage(WrongOne.class, CONSTRUCTOR_NAME, RightTwo.class)),
                String.format("%s and %s",
                        isNotDeclaredInMessage(WrongOne.class, "wrongMethod1", RightOne.class),
                        isNotDeclaredInMessage(WrongOne.class, "wrongMethod1", RightTwo.class)));
    }

    @DataProvider
    public static Object[][] ANDed_conditions() {
        return testForEach(members()
                .should(never(beDeclaredIn(WrongOne.class)))
                .andShould(never(beDeclaredIn(WrongTwo.class))));
        // FIXME: Add cases for fluent versions
    }

    @Test
    @UseDataProvider("ANDed_conditions")
    public void andShould_ANDs_conditions(ArchRule rule) {
        EvaluationResult result = rule
                .evaluate(importClasses(RightOne.class, RightTwo.class, WrongOne.class, WrongTwo.class));

        FailureReport report = result.getFailureReport();
        assertThat(report.toString())
                .contains(String.format(
                        "members should never be declared in %s and should never be declared in %s",
                        WrongOne.class.getName(), WrongTwo.class.getName()));
        assertThat(report.getDetails()).containsOnly(
                isDeclaredInMessage(WrongOne.class, CONSTRUCTOR_NAME, WrongOne.class),
                isDeclaredInMessage(WrongOne.class, "wrongMethod1", WrongOne.class),
                isDeclaredInMessage(WrongTwo.class, CONSTRUCTOR_NAME, WrongTwo.class),
                isDeclaredInMessage(WrongTwo.class, "wrongMethod2", WrongTwo.class));
    }

    private String isDeclaredInMessage(Class<?> clazz, String methodName, Class<?> expectedTarget) {
        return String.format("%s.%s is declared in %s",
                clazz.getSimpleName(), methodName, expectedTarget.getSimpleName());
    }

    private String isNotDeclaredInMessage(Class<?> clazz, String methodName, Class<?> expectedTarget) {
        return String.format("%s.%s is not declared in %s",
                clazz.getSimpleName(), methodName, expectedTarget.getSimpleName());
    }

    private static ArchCondition<JavaMember> beDeclaredIn(final Class<?> clazz) {
        return new ArchCondition<JavaMember>("be declared in " + clazz.getName()) {
            @Override
            public void check(JavaMember member, ConditionEvents events) {
                boolean satisfied = member.getOwner().isEquivalentTo(clazz);
                String message = String.format("%s.%s is %sdeclared in %s",
                        member.getOwner().getSimpleName(), member.getName(), satisfied ? "" : "not ", clazz.getSimpleName());
                events.add(new SimpleConditionEvent(member, satisfied, message));
            }
        };
    }

    @SuppressWarnings("unused")
    private static class RightOne {
        void rightMethod1() {
        }
    }

    @SuppressWarnings("unused")
    private static class RightTwo {
        void rightMethod2() {
        }
    }

    @SuppressWarnings("unused")
    private static class WrongOne {
        void wrongMethod1() {
        }
    }

    @SuppressWarnings("unused")
    private static class WrongTwo {
        void wrongMethod2() {
        }
    }
}