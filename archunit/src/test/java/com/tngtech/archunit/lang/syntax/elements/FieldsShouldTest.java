package com.tngtech.archunit.lang.syntax.elements;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.collect.Iterables.getOnlyElement;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.areNoFieldsWithType;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.assertViolation;
import static com.tngtech.archunit.lang.syntax.elements.MembersShouldTest.parseMembers;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class FieldsShouldTest {

    @Test
    public void complex_field_syntax() {
        EvaluationResult result = fields()
                .that(areNoFieldsWithType(List.class))
                .and().haveNameMatching(".*field(A|D).*")
                .should().beAnnotatedWith(A.class)
                .andShould().notBePublic()
                .orShould().haveRawType(Map.class)
                .evaluate(importClasses(ClassWithVariousMembers.class));

        assertViolation(result);
        String failure = getOnlyElement(result.getFailureReport().getDetails());
        assertThat(failure)
                .containsPattern(String.format("%s.* does not have raw type %s", FIELD_A, Map.class.getName()))
                .containsPattern(String.format("%s.* is not annotated with @%s", FIELD_A, A.class.getSimpleName()));
    }

    @DataProvider
    public static Object[][] restricted_property_rule_ends() {
        return testForEach(
                fields().should().haveRawType(String.class),
                fields().should().haveRawType(String.class.getName()),
                fields().should().haveRawType(equivalentTo(String.class).as(String.class.getName())));
    }

    @Test
    @UseDataProvider("restricted_property_rule_ends")
    public void property_predicates(ArchRule rule) {
        EvaluationResult result = rule
                .evaluate(importClasses(ClassWithVariousMembers.class));

        Set<String> actualFields = parseMembers(ClassWithVariousMembers.class, result.getFailureReport().getDetails());
        assertThat(actualFields).containsOnly(FIELD_B, FIELD_C, FIELD_D);
    }

    @DataProvider
    public static Object[][] restricted_property_rule_ends2() {
        return testForEach(
                fields().should().notHaveRawType(String.class),
                fields().should().notHaveRawType(String.class.getName()),
                fields().should().notHaveRawType(equivalentTo(String.class).as(String.class.getName())));
    }

    @Test
    @UseDataProvider("restricted_property_rule_ends2")
    public void property_predicates2(ArchRule rule) {
        EvaluationResult result = rule
                .evaluate(importClasses(ClassWithVariousMembers.class));

        Set<String> actualFields = parseMembers(ClassWithVariousMembers.class, result.getFailureReport().getDetails());
        assertThat(actualFields).containsOnly(FIELD_A);
    }

    private static final String FIELD_A = "fieldA";
    private static final String FIELD_B = "fieldB";
    private static final String FIELD_C = "fieldC";
    private static final String FIELD_D = "fieldD";

    @SuppressWarnings({"unused"})
    private static class ClassWithVariousMembers {
        private String fieldA;
        @A
        protected Object fieldB;
        public List<?> fieldC;
        Map<?, ?> fieldD;
    }

    private @interface A {
    }
}
