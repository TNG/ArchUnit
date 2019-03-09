package com.tngtech.archunit.lang.syntax.elements;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.domain.JavaMember;
import com.tngtech.archunit.core.domain.JavaMethod;
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

import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.union;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.equivalentTo;
import static com.tngtech.archunit.core.domain.JavaModifier.PUBLIC;
import static com.tngtech.archunit.core.domain.TestUtils.importClasses;
import static com.tngtech.archunit.core.domain.properties.HasModifiers.Predicates.modifier;
import static com.tngtech.archunit.lang.conditions.ArchPredicates.have;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.codeUnits;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.constructors;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.assertViolation;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.beAnnotatedWith;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.described;
import static com.tngtech.archunit.lang.syntax.elements.GivenMembersTest.everythingViolationPrintMemberName;
import static com.tngtech.java.junit.dataprovider.DataProviders.$;
import static com.tngtech.java.junit.dataprovider.DataProviders.$$;
import static java.util.Collections.emptySet;
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

    @Test
    public void types_match_for_methods() {
        EvaluationResult result = methods()
                // we can use the super type JavaMember
                .that(new DescribedPredicate<JavaMember>("are there") {
                    @Override
                    public boolean apply(JavaMember input) {
                        return true;
                    }
                })
                // and have preserved our type so we can filter JavaMethod later on
                .and(new DescribedPredicate<JavaMethod>("are there") {
                    @Override
                    public boolean apply(JavaMethod input) {
                        return true;
                    }
                })
                .should(new ArchCondition<JavaMethod>("not exist") {
                    @Override
                    public void check(JavaMethod method, ConditionEvents events) {
                        events.add(SimpleConditionEvent.violated(method, "expected violation"));
                    }
                })
                .evaluate(importClasses(ClassWithVariousMembers.class));

        assertThat(Joiner.on(" ").join(result.getFailureReport().getDetails())).contains("expected violation");
    }

    @Test
    public void types_match_for_constructors() {
        EvaluationResult result = constructors()
                // we can use the super type JavaMember
                .that(new DescribedPredicate<JavaMember>("are there") {
                    @Override
                    public boolean apply(JavaMember input) {
                        return true;
                    }
                })
                // and have preserved our type so we can filter JavaConstructor later on
                .and(new DescribedPredicate<JavaConstructor>("are there") {
                    @Override
                    public boolean apply(JavaConstructor input) {
                        return true;
                    }
                })
                .should(new ArchCondition<JavaConstructor>("not exist") {
                    @Override
                    public void check(JavaConstructor constructor, ConditionEvents events) {
                        events.add(SimpleConditionEvent.violated(constructor, "expected violation"));
                    }
                })
                .evaluate(importClasses(ClassWithVariousMembers.class));

        assertThat(Joiner.on(" ").join(result.getFailureReport().getDetails())).contains("expected violation");
    }

    @DataProvider
    public static Object[][] restricted_parameter_types_rule_starts() {
        return $$(
                $(described(codeUnits().that().haveRawParameterTypes(String.class)),
                        ImmutableSet.of(METHOD_ONE_ARG, CONSTRUCTOR_ONE_ARG)),
                $(described(codeUnits().that().haveRawParameterTypes(String.class.getName())),
                        ImmutableSet.of(METHOD_ONE_ARG, CONSTRUCTOR_ONE_ARG)),
                $(described(codeUnits().that().haveRawParameterTypes(oneParameterOfType(String.class))),
                        ImmutableSet.of(METHOD_ONE_ARG, CONSTRUCTOR_ONE_ARG)),

                $(described(codeUnits().that().doNotHaveRawParameterTypes(String.class)),
                        allCodeUnitsExcept(METHOD_ONE_ARG, CONSTRUCTOR_ONE_ARG)),
                $(described(codeUnits().that().doNotHaveRawParameterTypes(String.class.getName())),
                        allCodeUnitsExcept(METHOD_ONE_ARG, CONSTRUCTOR_ONE_ARG)),
                $(described(codeUnits().that().doNotHaveRawParameterTypes(oneParameterOfType(String.class))),
                        allCodeUnitsExcept(METHOD_ONE_ARG, CONSTRUCTOR_ONE_ARG)),

                $(described(methods().that().haveRawParameterTypes(String.class)),
                        ImmutableSet.of(METHOD_ONE_ARG)),
                $(described(methods().that().haveRawParameterTypes(String.class.getName())),
                        ImmutableSet.of(METHOD_ONE_ARG)),
                $(described(methods().that().haveRawParameterTypes(oneParameterOfType(String.class))),
                        ImmutableSet.of(METHOD_ONE_ARG)),

                $(described(methods().that().doNotHaveRawParameterTypes(String.class)),
                        allMethodsExcept(METHOD_ONE_ARG)),
                $(described(methods().that().doNotHaveRawParameterTypes(String.class.getName())),
                        allMethodsExcept(METHOD_ONE_ARG)),
                $(described(methods().that().doNotHaveRawParameterTypes(oneParameterOfType(String.class))),
                        allMethodsExcept(METHOD_ONE_ARG)),

                $(described(constructors().that().haveRawParameterTypes(String.class)),
                        ImmutableSet.of(CONSTRUCTOR_ONE_ARG)),
                $(described(constructors().that().haveRawParameterTypes(String.class.getName())),
                        ImmutableSet.of(CONSTRUCTOR_ONE_ARG)),
                $(described(constructors().that().haveRawParameterTypes(oneParameterOfType(String.class))),
                        ImmutableSet.of(CONSTRUCTOR_ONE_ARG)),

                $(described(constructors().that().doNotHaveRawParameterTypes(String.class)),
                        allConstructorsExcept(CONSTRUCTOR_ONE_ARG)),
                $(described(constructors().that().doNotHaveRawParameterTypes(String.class.getName())),
                        allConstructorsExcept(CONSTRUCTOR_ONE_ARG)),
                $(described(constructors().that().doNotHaveRawParameterTypes(oneParameterOfType(String.class))),
                        allConstructorsExcept(CONSTRUCTOR_ONE_ARG))
        );
    }

    @Test
    @UseDataProvider("restricted_parameter_types_rule_starts")
    public void parameter_types_predicates(DescribedRuleStart ruleStart, Collection<String> expectedMembers) {
        EvaluationResult result = ruleStart.should(everythingViolationPrintMemberName())
                .evaluate(importClasses(ClassWithVariousMembers.class));

        assertThat(result.getFailureReport().getDetails()).containsOnlyElementsOf(expectedMembers);
    }

    @DataProvider
    public static Object[][] restricted_return_type_rule_starts() {
        return $$(
                $(described(codeUnits().that().haveRawReturnType(String.class)), ImmutableList.of(METHOD_ONE_ARG, METHOD_THREE_ARGS)),
                $(described(codeUnits().that().haveRawReturnType(String.class.getName())), ImmutableList.of(METHOD_ONE_ARG, METHOD_THREE_ARGS)),
                $(described(codeUnits().that().haveRawReturnType(equivalentTo(String.class))), ImmutableList.of(METHOD_ONE_ARG, METHOD_THREE_ARGS)),

                $(described(codeUnits().that().doNotHaveRawReturnType(String.class)), allCodeUnitsExcept(METHOD_ONE_ARG, METHOD_THREE_ARGS)),
                $(described(codeUnits().that().doNotHaveRawReturnType(String.class.getName())),
                        allCodeUnitsExcept(METHOD_ONE_ARG, METHOD_THREE_ARGS)),
                $(described(codeUnits().that().doNotHaveRawReturnType(equivalentTo(String.class))),
                        allCodeUnitsExcept(METHOD_ONE_ARG, METHOD_THREE_ARGS)),

                $(described(methods().that().haveRawReturnType(String.class)), ImmutableList.of(METHOD_ONE_ARG, METHOD_THREE_ARGS)),
                $(described(methods().that().haveRawReturnType(String.class.getName())), ImmutableList.of(METHOD_ONE_ARG, METHOD_THREE_ARGS)),
                $(described(methods().that().haveRawReturnType(equivalentTo(String.class))), ImmutableList.of(METHOD_ONE_ARG, METHOD_THREE_ARGS)),

                $(described(methods().that().doNotHaveRawReturnType(String.class)), ImmutableList.of(METHOD_TWO_ARGS, METHOD_FOUR_ARGS)),
                $(described(methods().that().doNotHaveRawReturnType(String.class.getName())), ImmutableList.of(METHOD_TWO_ARGS, METHOD_FOUR_ARGS)),
                $(described(methods().that().doNotHaveRawReturnType(equivalentTo(String.class))),
                        ImmutableList.of(METHOD_TWO_ARGS, METHOD_FOUR_ARGS)),

                $(described(constructors().that().haveRawReturnType(void.class)), ALL_CONSTRUCTOR_DESCRIPTIONS),
                $(described(constructors().that().haveRawReturnType(void.class.getName())), ALL_CONSTRUCTOR_DESCRIPTIONS),
                $(described(constructors().that().haveRawReturnType(equivalentTo(void.class))), ALL_CONSTRUCTOR_DESCRIPTIONS),

                $(described(constructors().that().doNotHaveRawReturnType(void.class)), emptySet()),
                $(described(constructors().that().doNotHaveRawReturnType(void.class.getName())), emptySet()),
                $(described(constructors().that().doNotHaveRawReturnType(equivalentTo(void.class))), emptySet())
        );
    }

    @Test
    @UseDataProvider("restricted_return_type_rule_starts")
    public void return_type_predicates(DescribedRuleStart ruleStart, Collection<String> expectedMembers) {
        EvaluationResult result = ruleStart.should(everythingViolationPrintMemberName())
                .evaluate(importClasses(ClassWithVariousMembers.class));

        assertThat(result.getFailureReport().getDetails()).containsOnlyElementsOf(expectedMembers);
    }

    @DataProvider
    public static Object[][] restricted_throwable_type_rule_starts() {
        return $$(
                $(described(codeUnits().that().declareThrowableOfType(FirstException.class)),
                        ImmutableSet.of(METHOD_ONE_ARG, METHOD_THREE_ARGS, CONSTRUCTOR_ONE_ARG, CONSTRUCTOR_THREE_ARGS)),
                $(described(codeUnits().that().declareThrowableOfType(FirstException.class.getName())),
                        ImmutableSet.of(METHOD_ONE_ARG, METHOD_THREE_ARGS, CONSTRUCTOR_ONE_ARG, CONSTRUCTOR_THREE_ARGS)),
                $(described(codeUnits().that().declareThrowableOfType(equivalentTo(FirstException.class))),
                        ImmutableSet.of(METHOD_ONE_ARG, METHOD_THREE_ARGS, CONSTRUCTOR_ONE_ARG, CONSTRUCTOR_THREE_ARGS)),

                $(described(codeUnits().that().doNotDeclareThrowableOfType(FirstException.class)),
                        allCodeUnitsExcept(METHOD_ONE_ARG, METHOD_THREE_ARGS, CONSTRUCTOR_ONE_ARG, CONSTRUCTOR_THREE_ARGS)),
                $(described(codeUnits().that().doNotDeclareThrowableOfType(FirstException.class.getName())),
                        allCodeUnitsExcept(METHOD_ONE_ARG, METHOD_THREE_ARGS, CONSTRUCTOR_ONE_ARG, CONSTRUCTOR_THREE_ARGS)),
                $(described(codeUnits().that().doNotDeclareThrowableOfType(equivalentTo(FirstException.class))),
                        allCodeUnitsExcept(METHOD_ONE_ARG, METHOD_THREE_ARGS, CONSTRUCTOR_ONE_ARG, CONSTRUCTOR_THREE_ARGS)),

                $(described(methods().that().declareThrowableOfType(FirstException.class)),
                        ImmutableSet.of(METHOD_ONE_ARG, METHOD_THREE_ARGS)),
                $(described(methods().that().declareThrowableOfType(FirstException.class.getName())),
                        ImmutableSet.of(METHOD_ONE_ARG, METHOD_THREE_ARGS)),
                $(described(methods().that().declareThrowableOfType(equivalentTo(FirstException.class))),
                        ImmutableSet.of(METHOD_ONE_ARG, METHOD_THREE_ARGS)),

                $(described(methods().that().doNotDeclareThrowableOfType(FirstException.class)),
                        allMethodsExcept(METHOD_ONE_ARG, METHOD_THREE_ARGS)),
                $(described(methods().that().doNotDeclareThrowableOfType(FirstException.class.getName())),
                        allMethodsExcept(METHOD_ONE_ARG, METHOD_THREE_ARGS)),
                $(described(methods().that().doNotDeclareThrowableOfType(equivalentTo(FirstException.class))),
                        allMethodsExcept(METHOD_ONE_ARG, METHOD_THREE_ARGS)),

                $(described(constructors().that().declareThrowableOfType(FirstException.class)),
                        ImmutableSet.of(CONSTRUCTOR_ONE_ARG, CONSTRUCTOR_THREE_ARGS)),
                $(described(constructors().that().declareThrowableOfType(FirstException.class.getName())),
                        ImmutableSet.of(CONSTRUCTOR_ONE_ARG, CONSTRUCTOR_THREE_ARGS)),
                $(described(constructors().that().declareThrowableOfType(equivalentTo(FirstException.class))),
                        ImmutableSet.of(CONSTRUCTOR_ONE_ARG, CONSTRUCTOR_THREE_ARGS)),

                $(described(constructors().that().doNotDeclareThrowableOfType(FirstException.class)),
                        allConstructorsExcept(CONSTRUCTOR_ONE_ARG, CONSTRUCTOR_THREE_ARGS)),
                $(described(constructors().that().doNotDeclareThrowableOfType(FirstException.class.getName())),
                        allConstructorsExcept(CONSTRUCTOR_ONE_ARG, CONSTRUCTOR_THREE_ARGS)),
                $(described(constructors().that().doNotDeclareThrowableOfType(equivalentTo(FirstException.class))),
                        allConstructorsExcept(CONSTRUCTOR_ONE_ARG, CONSTRUCTOR_THREE_ARGS))
        );
    }

    @Test
    @UseDataProvider("restricted_throwable_type_rule_starts")
    public void throwable_type_predicates(DescribedRuleStart ruleStart, Collection<String> expectedMembers) {
        EvaluationResult result = ruleStart.should(everythingViolationPrintMemberName())
                .evaluate(importClasses(ClassWithVariousMembers.class));

        assertThat(result.getFailureReport().getDetails()).containsOnlyElementsOf(expectedMembers);
    }

    static DescribedPredicate<List<JavaClass>> oneParameterOfType(final Class<?> type) {
        return new DescribedPredicate<List<JavaClass>>("one parameter of type " + type.getName()) {
            @Override
            public boolean apply(List<JavaClass> input) {
                return input.size() == 1 && input.get(0).isEquivalentTo(type);
            }
        };
    }

    private static Set<String> allMethodsExcept(String... methodDescriptions) {
        return difference(ALL_METHOD_DESCRIPTIONS, ImmutableSet.copyOf(methodDescriptions));
    }

    private static Set<String> allConstructorsExcept(String... constructorDescriptions) {
        return difference(ALL_CONSTRUCTOR_DESCRIPTIONS, ImmutableSet.copyOf(constructorDescriptions));
    }

    private static Set<String> allCodeUnitsExcept(String... codeUnitDescriptions) {
        return difference(union(ALL_CONSTRUCTOR_DESCRIPTIONS, ALL_METHOD_DESCRIPTIONS), ImmutableSet.copyOf(codeUnitDescriptions));
    }

    static final String METHOD_ONE_ARG = "method(java.lang.String)";
    static final String METHOD_TWO_ARGS = "method(java.lang.String, java.lang.Object)";
    static final String METHOD_THREE_ARGS = "method(java.lang.String, java.lang.Object, java.util.List)";
    static final String METHOD_FOUR_ARGS = "method(java.lang.String, java.lang.Object, java.util.List, int)";
    static final Set<String> ALL_METHOD_DESCRIPTIONS =
            ImmutableSet.of(METHOD_ONE_ARG, METHOD_TWO_ARGS, METHOD_THREE_ARGS, METHOD_FOUR_ARGS);
    static final String CONSTRUCTOR_ONE_ARG = "<init>(java.lang.String)";
    static final String CONSTRUCTOR_TWO_ARGS = "<init>(java.lang.String, java.lang.Object)";
    private static final String CONSTRUCTOR_THREE_ARGS = "<init>(java.lang.String, java.lang.Object, java.util.List)";
    static final String CONSTRUCTOR_FOUR_ARGS = "<init>(java.lang.String, java.lang.Object, java.util.List, int)";
    private static final Set<String> ALL_CONSTRUCTOR_DESCRIPTIONS =
            ImmutableSet.of(CONSTRUCTOR_ONE_ARG, CONSTRUCTOR_TWO_ARGS, CONSTRUCTOR_THREE_ARGS, CONSTRUCTOR_FOUR_ARGS);

    private DescribedPredicate<List<JavaClass>> lessThanTwo() {
        return new DescribedPredicate<List<JavaClass>>("less than two") {
            @Override
            public boolean apply(List<JavaClass> input) {
                return input.size() < 2;
            }
        };
    }

    @SuppressWarnings({"unused", "RedundantThrows"})
    static class ClassWithVariousMembers {
        private ClassWithVariousMembers(String stringParam) throws FirstException {
        }

        @A
        protected ClassWithVariousMembers(String stringParam, Object objectParam) throws SecondException {
        }

        public ClassWithVariousMembers(String stringParam, Object objectParam, List<?> listParam) throws FirstException, SecondException {
        }

        ClassWithVariousMembers(String stringParam, Object objectParam, List<?> listParam, int intParam) {
        }

        private String method(String stringParam) throws FirstException {
            return null;
        }

        @A
        protected void method(String stringParam, Object objectParam) throws SecondException {
        }

        public String method(String stringParam, Object objectParam, List<?> listParam) throws FirstException, SecondException {
            return null;
        }

        void method(String stringParam, Object objectParam, List<?> listParam, int intParam) {
        }
    }

    @interface A {
    }

    static class FirstException extends Exception {
    }

    private static class SecondException extends Exception {
    }
}
