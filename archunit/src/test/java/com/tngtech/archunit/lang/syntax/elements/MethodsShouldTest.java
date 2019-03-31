package com.tngtech.archunit.lang.syntax.elements;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Collection;
import java.util.Set;

import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.elements.MembersShouldTest.parseMembers;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class MethodsShouldTest {

    @DataProvider
    public static Object[][] restricted_property_rule_ends() {
        return $$(
                $(methods().should().beFinal(), ImmutableSet.of(METHOD_C, METHOD_D)),
                $(methods().should().notBeFinal(), ImmutableSet.of(METHOD_A, METHOD_B)),
                $(methods().should().beStatic(), ImmutableSet.of(METHOD_A, METHOD_C)),
                $(methods().should().notBeStatic(), ImmutableSet.of(METHOD_B, METHOD_D)),
                $(methods().should().notBeFinal().andShould().notBeStatic(), ImmutableSet.of(METHOD_A, METHOD_B, METHOD_D)),
                $(methods().should().notBeFinal().orShould().notBeStatic(), ImmutableSet.of(METHOD_B))
        );
    }

    @Test
    @UseDataProvider("restricted_property_rule_ends")
    public void property_predicates(ArchRule ruleStart, Collection<String> expectedMembers) {
        EvaluationResult result = ruleStart.evaluate(importClasses(ClassWithVariousMembers.class));

        Set<String> actualMethods = parseMembers(ClassWithVariousMembers.class, result.getFailureReport().getDetails());
        assertThat(actualMethods).containsOnlyElementsOf(expectedMembers);
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
