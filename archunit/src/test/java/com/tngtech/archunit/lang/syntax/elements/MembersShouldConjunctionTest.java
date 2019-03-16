package com.tngtech.archunit.lang.syntax.elements;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.FailureReport;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.conditions.ArchConditions.beDeclaredIn;
import static com.tngtech.archunit.lang.conditions.ArchConditions.not;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.members;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class MembersShouldConjunctionTest {
    @DataProvider
    public static Object[][] ORed_conditions() {
        return testForEach(
                members()
                        .should(beDeclaredIn(RightOne.class))
                        .orShould(beDeclaredIn(RightTwo.class)),
                members()
                        .should().beDeclaredIn(RightOne.class)
                        .orShould().beDeclaredIn(RightTwo.class)
        );
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
        return testForEach(
                members()
                        .should(not(beDeclaredIn(WrongOne.class)))
                        .andShould(not(beDeclaredIn(WrongTwo.class))),
                members()
                        .should().notBeDeclaredIn(WrongOne.class)
                        .andShould().notBeDeclaredIn(WrongTwo.class)
        );
    }

    @Test
    @UseDataProvider("ANDed_conditions")
    public void andShould_ANDs_conditions(ArchRule rule) {
        EvaluationResult result = rule
                .evaluate(importClasses(RightOne.class, RightTwo.class, WrongOne.class, WrongTwo.class));

        FailureReport report = result.getFailureReport();
        assertThat(report.toString())
                .contains(String.format(
                        "members should not be declared in %s and should not be declared in %s",
                        WrongOne.class.getName(), WrongTwo.class.getName()));
        assertThat(report.getDetails()).containsOnly(
                isDeclaredInMessage(WrongOne.class, CONSTRUCTOR_NAME),
                isDeclaredInMessage(WrongOne.class, "wrongMethod1"),
                isDeclaredInMessage(WrongTwo.class, CONSTRUCTOR_NAME),
                isDeclaredInMessage(WrongTwo.class, "wrongMethod2"));
    }

    private String isDeclaredInMessage(Class<?> clazz, String methodName) {
        return message(clazz, methodName, "", clazz);
    }

    private String isNotDeclaredInMessage(Class<?> clazz, String methodName, Class<?> expectedTarget) {
        return message(clazz, methodName, "not ", expectedTarget);
    }

    private String message(Class<?> clazz, String methodName, String optionalNot, Class<?> expectedTarget) {
        return String.format("%s <%s.%s()> is %sdeclared in %s in (%s.java:0)",
                CONSTRUCTOR_NAME.equals(methodName) ? "Constructor" : "Method",
                clazz.getName(), methodName, optionalNot, expectedTarget.getName(), getClass().getSimpleName());
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