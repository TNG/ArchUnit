package com.tngtech.archunit.lang.syntax.elements;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.*;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;

import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.*;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class GivenMethodsTest {

    @DataProvider
    public static Object[][] restricted_property_rule_starts() {
        return $$(
                $(described(methods().that().areFinal()), ImmutableSet.of(METHOD_A, METHOD_B)),
                $(described(methods().that().areNotFinal()), ImmutableSet.of(METHOD_C, METHOD_D)),
                $(described(methods().that().areStatic()), ImmutableSet.of(METHOD_B, METHOD_D)),
                $(described(methods().that().areNotStatic()), ImmutableSet.of(METHOD_A, METHOD_C)),
                $(described(methods().that().areFinal().and().areStatic()), ImmutableSet.of(METHOD_B)),
                $(described(methods().that().areFinal().or().areStatic()), ImmutableSet.of(METHOD_A, METHOD_B, METHOD_D))
        );
    }

    @Test
    @UseDataProvider("restricted_property_rule_starts")
    public void property_predicates(DescribedRuleStart ruleStart, Collection<String> expectedMembers) {
        EvaluationResult result = ruleStart.should(everythingViolationPrintMemberName())
                .evaluate(importClasses(ClassWithVariousMembers.class));

        assertThat(result.getFailureReport().getDetails()).containsOnlyElementsOf(expectedMembers);
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
