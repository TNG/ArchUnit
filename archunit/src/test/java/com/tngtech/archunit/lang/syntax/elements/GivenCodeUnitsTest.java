package com.tngtech.archunit.lang.syntax.elements;

import java.util.List;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
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
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.core.domain.properties.HasModifiers.Predicates.modifier;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.codeUnits;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.assertViolation;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.beAnnotatedWith;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.described;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.formatMember;
import static com.tngtech.java.junit.dataprovider.DataProviders.testForEach;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(DataProviderRunner.class)
public class GivenCodeUnitsTest {

    @Test
    public void complex_code_unit_syntax() {
        EvaluationResult result = codeUnits()
                .that().areNotPackagePrivate()
                .and().haveRawParameterTypes(lessThanTwo())
                .or(have(modifier(PUBLIC)))
                .should(beAnnotatedWith(A.class))
                .evaluate(importClasses(ClassWithVariousMembers.class));

        assertViolation(result);
        assertThat(result.getFailureReport().getDetails()).containsOnly(
                String.format("Member '%s' is not annotated with @A", METHOD_ONE_ARG),
                String.format("Member '%s' is not annotated with @A", METHOD_THREE_ARGS),
                String.format("Member '%s' is not annotated with @A", CONSTRUCTOR_ONE_ARG),
                String.format("Member '%s' is not annotated with @A", CONSTRUCTOR_THREE_ARGS));
    }

    @DataProvider
    public static Object[][] restricted_parameter_types_rule_starts() {
        return testForEach(
                described(codeUnits().that().haveRawParameterTypes(String.class)),
                described(codeUnits().that().haveRawParameterTypes(String.class.getName())),
                described(codeUnits().that().haveRawParameterTypes(oneParameterOfType(String.class))));
    }

    @Test
    @UseDataProvider("restricted_parameter_types_rule_starts")
    public void parameter_types_predicates(DescribedRuleStart ruleStart) {
        EvaluationResult result = ruleStart.should(new ArchCondition<JavaMember>("condition text") {
            @Override
            public void check(JavaMember item, ConditionEvents events) {
                events.add(SimpleConditionEvent.violated(item, formatMember(item)));
            }
        }).evaluate(importClasses(ClassWithVariousMembers.class));

        assertThat(result.getFailureReport().getDetails()).containsOnly(METHOD_ONE_ARG, CONSTRUCTOR_ONE_ARG);
    }

    @DataProvider
    public static Object[][] restricted_return_type_rule_starts() {
        return testForEach(
                described(codeUnits().that().haveRawReturnType(String.class)),
                described(codeUnits().that().haveRawReturnType(String.class.getName())),
                described(codeUnits().that().haveRawReturnType(equivalentTo(String.class))));
    }

    @Test
    @UseDataProvider("restricted_return_type_rule_starts")
    public void return_type_predicates(DescribedRuleStart ruleStart) {
        EvaluationResult result = ruleStart.should(new ArchCondition<JavaMember>("condition text") {
            @Override
            public void check(JavaMember item, ConditionEvents events) {
                events.add(SimpleConditionEvent.violated(item, formatMember(item)));
            }
        }).evaluate(importClasses(ClassWithVariousMembers.class));

        assertThat(result.getFailureReport().getDetails()).containsOnly(METHOD_ONE_ARG, METHOD_THREE_ARGS);
    }

    private static DescribedPredicate<List<JavaClass>> oneParameterOfType(final Class<?> type) {
        return new DescribedPredicate<List<JavaClass>>("one parameter of type " + type.getName()) {
            @Override
            public boolean apply(List<JavaClass> input) {
                return input.size() == 1 && input.get(0).isEquivalentTo(type);
            }
        };
    }

    private static final String METHOD_ONE_ARG = "method(java.lang.String)";
    private static final String METHOD_THREE_ARGS = "method(java.lang.String, java.lang.Object, java.util.List)";
    private static final String CONSTRUCTOR_ONE_ARG = "<init>(java.lang.String)";
    private static final String CONSTRUCTOR_THREE_ARGS = "<init>(java.lang.String, java.lang.Object, java.util.List)";

    private DescribedPredicate<List<JavaClass>> lessThanTwo() {
        return new DescribedPredicate<List<JavaClass>>("less than two") {
            @Override
            public boolean apply(List<JavaClass> input) {
                return input.size() < 2;
            }
        };
    }

    @SuppressWarnings({"unused"})
    private static class ClassWithVariousMembers {
        private ClassWithVariousMembers(String stringParam) {
        }

        @A
        protected ClassWithVariousMembers(String stringParam, Object objectParam) {
        }

        public ClassWithVariousMembers(String stringParam, Object objectParam, List<?> listParam) {
        }

        ClassWithVariousMembers(String stringParam, Object objectParam, List<?> listParam, int intParam) {
        }

        private String method(String stringParam) {
            return null;
        }

        @A
        protected void method(String stringParam, Object objectParam) {
        }

        public String method(String stringParam, Object objectParam, List<?> listParam) {
            return null;
        }

        void method(String stringParam, Object objectParam, List<?> listParam, int intParam) {
        }
    }

    private @interface A {
    }
}
