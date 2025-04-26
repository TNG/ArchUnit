package com.tngtech.archunit.lang.syntax.elements;

import java.util.stream.Stream;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.FailureReport;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.core.domain.JavaConstructor.CONSTRUCTOR_NAME;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.conditions.ArchConditions.beDeclaredIn;
import static com.tngtech.archunit.lang.conditions.ArchConditions.not;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.members;
import static org.assertj.core.api.Assertions.assertThat;

public class MembersShouldConjunctionTest {
    static Stream<ArchRule> ORed_conditions() {
        return Stream.of(
                members()
                        .should(beDeclaredIn(RightOne.class))
                        .orShould(beDeclaredIn(RightTwo.class)),
                members()
                        .should().beDeclaredIn(RightOne.class)
                        .orShould().beDeclaredIn(RightTwo.class)
        );
    }

    @ParameterizedTest
    @MethodSource("ORed_conditions")
    void orShould_ORs_conditions(ArchRule rule) {
        EvaluationResult result = rule
                .evaluate(importClasses(RightOne.class, RightTwo.class, WrongOne.class));

        FailureReport report = result.getFailureReport();
        assertThat(report.toString())
                .contains(String.format(
                        "members should be declared in %s or should be declared in %s",
                        RightOne.class.getName(), RightTwo.class.getName()));
        assertThat(report.getDetails()).containsOnly(
                String.format("%s and %s",
                        isNotDeclaredInMessage(WrongOne.class, CONSTRUCTOR_NAME, RightOne.class, 106),
                        isNotDeclaredInMessage(WrongOne.class, CONSTRUCTOR_NAME, RightTwo.class, 106)),
                String.format("%s and %s",
                        isNotDeclaredInMessage(WrongOne.class, "wrongMethod1", RightOne.class, 108),
                        isNotDeclaredInMessage(WrongOne.class, "wrongMethod1", RightTwo.class, 108)));
    }

    static Stream<ArchRule> ANDed_conditions() {
        return Stream.of(
                members()
                        .should(not(beDeclaredIn(WrongOne.class)))
                        .andShould(not(beDeclaredIn(WrongTwo.class))),
                members()
                        .should().notBeDeclaredIn(WrongOne.class)
                        .andShould().notBeDeclaredIn(WrongTwo.class)
        );
    }

    @ParameterizedTest
    @MethodSource("ANDed_conditions")
    void andShould_ANDs_conditions(ArchRule rule) {
        EvaluationResult result = rule
                .evaluate(importClasses(RightOne.class, RightTwo.class, WrongOne.class, WrongTwo.class));

        FailureReport report = result.getFailureReport();
        assertThat(report.toString())
                .contains(String.format(
                        "members should not be declared in %s and should not be declared in %s",
                        WrongOne.class.getName(), WrongTwo.class.getName()));
        assertThat(report.getDetails()).containsOnly(
                isDeclaredInMessage(WrongOne.class, CONSTRUCTOR_NAME, 106),
                isDeclaredInMessage(WrongOne.class, "wrongMethod1", 108),
                isDeclaredInMessage(WrongTwo.class, CONSTRUCTOR_NAME, 112),
                isDeclaredInMessage(WrongTwo.class, "wrongMethod2", 114));
    }

    private String isDeclaredInMessage(Class<?> clazz, String methodName, int lineNumber) {
        return message(clazz, methodName, "", clazz, lineNumber);
    }

    private String isNotDeclaredInMessage(Class<?> clazz, String methodName, Class<?> expectedTarget, int lineNumber) {
        return message(clazz, methodName, "not ", expectedTarget, lineNumber);
    }

    private String message(Class<?> clazz, String methodName, String optionalNot, Class<?> expectedTarget, int lineNumber) {
        return String.format("%s <%s.%s()> is %sdeclared in %s in (%s.java:%d)",
                CONSTRUCTOR_NAME.equals(methodName) ? "Constructor" : "Method",
                clazz.getName(), methodName, optionalNot, expectedTarget.getName(), getClass().getSimpleName(), lineNumber);
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