package com.tngtech.archunit.lang.syntax.elements;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.DescribedRuleStart;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.common.collect.Sets.difference;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_RAW_TYPE;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.assertViolation;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.beAnnotatedWith;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.described;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.everythingViolationPrintMemberName;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class GivenFieldsTest {

    @Test
    public void complex_field_syntax() {
        EvaluationResult result = fields()
                .that().haveNameMatching("field(A|D)")
                .and().haveRawType(String.class)
                .or(GET_RAW_TYPE.is(type(List.class)))
                .should(beAnnotatedWith(A.class))
                .evaluate(importClasses(ClassWithVariousMembers.class));

        assertViolation(result);
        assertThat(result.getFailureReport().getDetails()).containsOnly(
                String.format("Member '%s' is not annotated with @A", FIELD_A),
                String.format("Member '%s' is not annotated with @A", FIELD_C));
    }

    @DataProvider
    public static Object[][] restricted_property_rule_starts() {
        return $$(
                $(described(fields().that().haveRawType(String.class)), ImmutableSet.of(FIELD_A)),
                $(described(fields().that().haveRawType(String.class.getName())), ImmutableSet.of(FIELD_A)),
                $(described(fields().that().haveRawType(equivalentTo(String.class))), ImmutableSet.of(FIELD_A)),

                $(described(fields().that().doNotHaveRawType(String.class)), allFieldsExcept(FIELD_A)),
                $(described(fields().that().doNotHaveRawType(String.class.getName())), allFieldsExcept(FIELD_A)),
                $(described(fields().that().doNotHaveRawType(equivalentTo(String.class))), allFieldsExcept(FIELD_A)),

                $(described(fields().that().areFinal()), ImmutableSet.of(FIELD_A, FIELD_B)),
                $(described(fields().that().areNotFinal()), ImmutableSet.of(FIELD_C, FIELD_D)),
                $(described(fields().that().areStatic()), ImmutableSet.of(FIELD_B, FIELD_D)),
                $(described(fields().that().areNotStatic()), ImmutableSet.of(FIELD_A, FIELD_C)),

                $(described(fields().that().areStatic().and().areFinal()), ImmutableSet.of(FIELD_B))
        );
    }

    @Test
    @UseDataProvider("restricted_property_rule_starts")
    public void property_predicates(DescribedRuleStart ruleStart, Collection<String> expectedMembers) {
        EvaluationResult result = ruleStart.should(everythingViolationPrintMemberName())
                .evaluate(importClasses(ClassWithVariousMembers.class));

        assertThat(result.getFailureReport().getDetails()).containsOnlyElementsOf(expectedMembers);
    }

    private static Set<String> allFieldsExcept(String... fieldNames) {
        return difference(ALL_FIELD_DESCRIPTIONS, ImmutableSet.copyOf(fieldNames));
    }

    private static final String FIELD_A = "fieldA";
    private static final String FIELD_B = "fieldB";
    private static final String FIELD_C = "fieldC";
    private static final String FIELD_D = "fieldD";
    private static final Set<String> ALL_FIELD_DESCRIPTIONS = ImmutableSet.of(FIELD_A, FIELD_B, FIELD_C, FIELD_D);

    @SuppressWarnings({"unused"})
    private static class ClassWithVariousMembers {
        private final String fieldA = "A";
        @A
        protected static final Object fieldB = 'B';
        public List<?> fieldC;
        static Map<?, ?> fieldD;
    }

    private @interface A {
    }
}
