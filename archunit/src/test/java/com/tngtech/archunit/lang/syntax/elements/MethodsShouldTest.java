package com.tngtech.archunit.lang.syntax.elements;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.elements.MembersShouldTest.parseMembers;
import static com.tngtech.archunit.testutil.DataProviders.$;
import static org.assertj.core.api.Assertions.assertThat;

public class MethodsShouldTest {

    static Stream<Arguments> restricted_property_rule_ends() {
        return Stream.of(
                $(methods().should().beFinal(), ImmutableSet.of(METHOD_C, METHOD_D)),
                $(methods().should().notBeFinal(), ImmutableSet.of(METHOD_A, METHOD_B)),
                $(methods().should().beStatic(), ImmutableSet.of(METHOD_A, METHOD_C)),
                $(methods().should().notBeStatic(), ImmutableSet.of(METHOD_B, METHOD_D)),
                $(methods().should().notBeFinal().andShould().notBeStatic(), ImmutableSet.of(METHOD_A, METHOD_B, METHOD_D)),
                $(methods().should().notBeFinal().orShould().notBeStatic(), ImmutableSet.of(METHOD_B))
        );
    }

    @ParameterizedTest
    @MethodSource("restricted_property_rule_ends")
    void property_predicates(ArchRule ruleStart, Collection<String> expectedMembers) {
        EvaluationResult result = ruleStart.evaluate(importClasses(ClassWithVariousMembers.class));

        Set<String> actualMethods = parseMembers(ClassWithVariousMembers.class, result.getFailureReport().getDetails());
        assertThat(actualMethods).hasSameElementsAs(expectedMembers);
    }

    private static final String METHOD_A = "methodA([I)";
    private static final String METHOD_B = "methodB(boolean)";
    private static final String METHOD_C = "methodC(char)";
    private static final String METHOD_D = "methodD()";

    @SuppressWarnings({"unused"})
    private static class ClassWithVariousMembers {
        public final void methodA(int[] array) {
        }
        protected static final void methodB(boolean flag) {
        }
        private void methodC(char ch) {
        }
        static int methodD() {
            return 0;
        }
    }
}
