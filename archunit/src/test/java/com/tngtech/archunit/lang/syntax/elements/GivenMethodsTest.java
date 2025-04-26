package com.tngtech.archunit.lang.syntax.elements;

import java.util.Collection;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.DescribedRuleStart;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.described;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.everythingViolationPrintMemberName;
import static com.tngtech.archunit.testutil.DataProviders.$;
import static org.assertj.core.api.Assertions.assertThat;

public class GivenMethodsTest {

    static Stream<Arguments> restricted_property_rule_starts() {
        return Stream.of(
                $(described(methods().that().areFinal()), ImmutableSet.of(METHOD_A, METHOD_B)),
                $(described(methods().that().areNotFinal()), ImmutableSet.of(METHOD_C, METHOD_D)),
                $(described(methods().that().areStatic()), ImmutableSet.of(METHOD_B, METHOD_D)),
                $(described(methods().that().areNotStatic()), ImmutableSet.of(METHOD_A, METHOD_C)),
                $(described(methods().that().areFinal().and().areStatic()), ImmutableSet.of(METHOD_B)),
                $(described(methods().that().areFinal().or().areStatic()), ImmutableSet.of(METHOD_A, METHOD_B, METHOD_D))
        );
    }

    @ParameterizedTest
    @MethodSource("restricted_property_rule_starts")
    void property_predicates(DescribedRuleStart ruleStart, Collection<String> expectedMembers) {
        EvaluationResult result = ruleStart.should(everythingViolationPrintMemberName())
                .evaluate(importClasses(ClassWithVariousMembers.class));

        assertThat(result.getFailureReport().getDetails()).hasSameElementsAs(expectedMembers);
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
