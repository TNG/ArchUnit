package com.tngtech.archunit.lang.syntax.elements;

import java.util.List;
import java.util.Map;

import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.DescribedRuleStart;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.type;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.core.domain.properties.HasType.Functions.GET_TYPE;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.assertViolation;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.beAnnotatedWith;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.described;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.formatMember;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class GivenFieldsTest {

    @Test
    public void complex_field_syntax() {
        EvaluationResult result = fields()
                .that().haveNameMatching("field(A|D)")
                .and().haveType(String.class)
                .or(GET_TYPE.is(type(List.class)))
                .should(beAnnotatedWith(A.class))
                .evaluate(importClasses(ClassWithVariousMembers.class));

        assertViolation(result);
        assertThat(result.getFailureReport().getDetails()).containsOnly(
                String.format("Member '%s' is not annotated with @A", FIELD_A),
                String.format("Member '%s' is not annotated with @A", FIELD_C));
    }

    @DataProvider
    public static Object[][] restricted_property_rule_starts() {
        return testForEach(
                described(fields().that().haveType(String.class)),
                described(fields().that().haveType(String.class.getName())),
                described(fields().that().haveType(equivalentTo(String.class))));
    }

    @Test
    @UseDataProvider("restricted_property_rule_starts")
    public void property_predicates(DescribedRuleStart ruleStart) {
        EvaluationResult result = ruleStart.should(new ArchCondition<JavaMember>("condition text") {
            @Override
            public void check(JavaMember item, ConditionEvents events) {
                events.add(SimpleConditionEvent.violated(item, formatMember(item)));
            }
        }).evaluate(importClasses(ClassWithVariousMembers.class));

        assertThat(result.getFailureReport().getDetails()).containsOnly(FIELD_A);
    }

    private static final String FIELD_A = "fieldA";
    private static final String FIELD_C = "fieldC";

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
